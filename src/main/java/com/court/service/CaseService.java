package com.court.service;

import com.court.dao.AuditLogDao;
import com.court.dao.CaseDao;
import com.court.dao.HearingDao;
import com.court.dao.OrderDao;
import com.court.dao.PartyDao;
import com.court.dao.impl.AuditLogDaoImpl;
import com.court.dao.impl.CaseDaoImpl;
import com.court.dao.impl.HearingDaoImpl;
import com.court.dao.impl.OrderDaoImpl;
import com.court.dao.impl.PartyDaoImpl;
import com.court.exception.InvalidStatusTransitionException;
import com.court.exception.ValidationException;
import com.court.model.CaseDocument;
import com.court.model.CaseStatus;
import com.court.model.CaseType;
import com.court.model.CourtCase;
import com.court.model.CourtOrder;
import com.court.model.Hearing;
import com.court.model.Judge;
import com.court.model.Lawyer;
import com.court.model.OrderOutcome;
import com.court.model.Party;
import com.court.model.PartyType;
import com.court.util.DBConnection;
import com.court.util.SessionManager;
import com.court.util.Validators;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * The life of a case: filing, scrutiny, registration, correction and judgment.
 */
public class CaseService {

    private static final Logger LOG = Logger.getLogger(CaseService.class.getName());

    private final CaseDao caseDao = new CaseDaoImpl();
    private final PartyDao partyDao = new PartyDaoImpl();
    private final HearingDao hearingDao = new HearingDaoImpl();
    private final OrderDao orderDao = new OrderDaoImpl();
    private final AuditLogDao auditLogDao = new AuditLogDaoImpl();
    private final DocumentService documentService = new DocumentService();
    private final HearingService hearingService = new HearingService();

    /** Everything about one case, for the case details window. */
    public record CaseDetails(CourtCase courtCase, List<Hearing> hearings, List<CourtOrder> orders,
                              List<CaseDocument> documents) {
    }

    /** What a lawyer types into the filing form. */
    public record FilingForm(CaseType type, String description, boolean urgent, List<Party> parties,
                             List<Path> newDocuments) {
    }

    // ------------------------------------------------------------------ queries

    public List<CourtCase> findByStatus(CaseStatus status) {
        return caseDao.findByStatus(status);
    }

    public List<CourtCase> findByLawyer(int lawyerId) {
        return caseDao.findByLawyer(lawyerId);
    }

    public List<CourtCase> findByJudge(int judgeId) {
        return caseDao.findByJudge(judgeId);
    }

    public List<CourtCase> findActive() {
        return caseDao.findActive();
    }

    public List<CourtCase> findAll() {
        return caseDao.findAll();
    }

    public Optional<CourtCase> findWithParties(int caseId) {
        return caseDao.findWithParties(caseId);
    }

    public CaseDetails details(int caseId) throws ValidationException {
        CourtCase c = caseDao.findWithParties(caseId)
                .orElseThrow(() -> new ValidationException("This case no longer exists."));
        return new CaseDetails(c, hearingDao.findByCase(caseId), orderDao.findByCase(caseId),
                documentService.findByCase(caseId));
    }

    public List<CourtOrder> ordersForLawyer(int lawyerId) {
        return orderDao.findByLawyer(lawyerId);
    }

    // ------------------------------------------------------------------ filing (lawyer)

    /** Files a new case with its parties and documents in one transaction. */
    public CourtCase fileCase(Lawyer lawyer, FilingForm form) throws ValidationException {
        CourtCase c = new CourtCase();
        fillFromForm(c, form);
        c.setFiledById(lawyer.getId());
        c.setFiledByName(lawyer.getName());
        c.setFilingDate(LocalDate.now());

        int id = DBConnection.inTransaction(con -> {
            int newId = caseDao.insert(con, c);
            partyDao.insertAll(con, newId, c.getParties());
            for (Path file : form.newDocuments()) {
                documentService.store(con, newId, file, lawyer.getId());
            }
            return newId;
        });
        c.setId(id);
        LOG.info("Case filed: " + c.getTitle() + " by " + lawyer.getUsername());
        audit("CASE_FILED", c.getTitle());
        return c;
    }

    /** Saves a corrected filing (after it was returned as defective) and sends it back to scrutiny. */
    public void resubmit(CourtCase c, Lawyer lawyer, FilingForm form) throws ValidationException {
        if (c.getStatus() != CaseStatus.DEFECTIVE) {
            throw new ValidationException("Only filings returned as defective can be corrected and filed again.");
        }
        fillFromForm(c, form);
        DBConnection.inTransaction(con -> {
            caseDao.updateFiling(con, c);
            partyDao.deleteByCase(con, c.getId());
            partyDao.insertAll(con, c.getId(), c.getParties());
            for (Path file : form.newDocuments()) {
                documentService.store(con, c.getId(), file, lawyer.getId());
            }
            return null;
        });
        audit("CASE_REFILED", c.getTitle());
    }

    /** Validates the form and copies it into the case. Title is "first petitioner vs. first respondent". */
    private void fillFromForm(CourtCase c, FilingForm form) throws ValidationException {
        if (form.type() == null) {
            throw new ValidationException("Choose the case type.");
        }
        List<Party> parties = new ArrayList<>();
        for (Party p : form.parties()) {
            if (p.name().isBlank()) continue;
            String name = Validators.required(p.name(), p.type().getDisplayName() + " name", 100);
            String phone = Validators.phone(p.phone());
            parties.add(new Party(name, p.type(), Validators.optional(p.address()), phone));
        }
        Party petitioner = parties.stream().filter(p -> p.type() == PartyType.PETITIONER).findFirst()
                .orElseThrow(() -> new ValidationException("Add at least one petitioner."));
        Party respondent = parties.stream().filter(p -> p.type() == PartyType.RESPONDENT).findFirst()
                .orElseThrow(() -> new ValidationException("Add at least one respondent."));

        String description = Validators.minLength(form.description(), "Case details", 20, 2000);
        if (form.newDocuments().size() > DocumentService.MAX_FILES) {
            throw new ValidationException("Attach at most " + DocumentService.MAX_FILES + " documents.");
        }
        for (Path file : form.newDocuments()) {
            documentService.check(file);
        }

        c.setTitle(petitioner.name() + " vs. " + respondent.name());
        c.setType(form.type());
        c.setDescription(description);
        c.setUrgent(form.urgent());
        c.getParties().clear();
        c.getParties().addAll(parties);
    }

    // ------------------------------------------------------------------ scrutiny (registrar)

    /**
     * Registers a filed case: gives it a case number, assigns the judge and courtroom and
     * lists the first hearing - all in one transaction.
     *
     * @return the new case number
     */
    public String register(CourtCase c, Judge judge, LocalDate firstHearing, LocalTime slot) throws ValidationException {
        if (!c.getStatus().canMoveTo(CaseStatus.REGISTERED)) {
            throw new InvalidStatusTransitionException(c.getStatus(), CaseStatus.REGISTERED);
        }
        if (judge == null || !judge.hasCourtroom()) {
            throw new ValidationException("Choose a judge who has a courtroom.");
        }
        hearingService.validateSlot(judge.getId(), judge.getCourtroomId(), c.getFiledById(), firstHearing, slot, null);

        int year = LocalDate.now().getYear();
        String caseNumber = CaseNumbers.next(c.getType(), year, caseDao.caseNumbersFor(c.getType(), year));
        DBConnection.inTransaction(con -> {
            caseDao.register(con, c.getId(), caseNumber, judge.getId(), judge.getCourtroomId(),
                    LocalDate.now(), firstHearing);
            hearingDao.insert(con, c.getId(), judge.getId(), judge.getCourtroomId(), firstHearing, slot);
            return null;
        });
        c.moveTo(CaseStatus.REGISTERED);
        c.setCaseNumber(caseNumber);
        LOG.info("Registered " + caseNumber + " (" + c.getTitle() + ")");
        audit("CASE_REGISTERED", caseNumber + " - " + c.getTitle());
        return caseNumber;
    }

    public void returnAsDefective(CourtCase c, String reason) throws ValidationException {
        if (!c.getStatus().canMoveTo(CaseStatus.DEFECTIVE)) {
            throw new InvalidStatusTransitionException(c.getStatus(), CaseStatus.DEFECTIVE);
        }
        String text = Validators.minLength(reason, "Reason", 10, 500);
        caseDao.markDefective(c.getId(), text);
        c.moveTo(CaseStatus.DEFECTIVE);
        audit("CASE_RETURNED", c.getTitle() + ": " + text);
    }

    // ------------------------------------------------------------------ judgment (judge)

    /** Delivers the final judgment in a reserved (or pending) case and closes it. */
    public void deliverJudgment(CourtCase c, int judgeId, String judgment) throws ValidationException {
        if (!c.getStatus().canMoveTo(CaseStatus.DISPOSED)) {
            throw new InvalidStatusTransitionException(c.getStatus(), CaseStatus.DISPOSED);
        }
        String text = Validators.minLength(judgment, "Judgment", 20, 8000);
        LocalDate today = LocalDate.now();
        DBConnection.inTransaction(con -> {
            orderDao.insert(con, c.getId(), null, judgeId, text, OrderOutcome.DISPOSED, null, today);
            caseDao.dispose(con, c.getId(), text, today);
            hearingDao.cancelScheduled(con, c.getId());
            return null;
        });
        c.moveTo(CaseStatus.DISPOSED);
        audit("JUDGMENT_DELIVERED", c.getCaseNumber());
    }

    private void audit(String action, String details) {
        Integer by = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : null;
        auditLogDao.log(by, action, details);
    }
}
