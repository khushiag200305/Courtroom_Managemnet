package com.court.service;

import com.court.dao.AuditLogDao;
import com.court.dao.CaseDao;
import com.court.dao.HearingDao;
import com.court.dao.OrderDao;
import com.court.dao.ReportDao;
import com.court.dao.impl.AuditLogDaoImpl;
import com.court.dao.impl.CaseDaoImpl;
import com.court.dao.impl.HearingDaoImpl;
import com.court.dao.impl.OrderDaoImpl;
import com.court.dao.impl.ReportDaoImpl;
import com.court.exception.InvalidStatusTransitionException;
import com.court.exception.ScheduleConflictException;
import com.court.exception.ValidationException;
import com.court.model.CaseStatus;
import com.court.model.CourtCase;
import com.court.model.Hearing;
import com.court.model.HearingStatus;
import com.court.model.OrderOutcome;
import com.court.util.AppConfig;
import com.court.util.DBConnection;
import com.court.util.SessionManager;
import com.court.util.Validators;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Scheduling hearings and recording what happened at them.
 */
public class HearingService {

    private static final Logger LOG = Logger.getLogger(HearingService.class.getName());

    private final HearingDao hearingDao = new HearingDaoImpl();
    private final CaseDao caseDao = new CaseDaoImpl();
    private final OrderDao orderDao = new OrderDaoImpl();
    private final ReportDao reportDao = new ReportDaoImpl();
    private final AuditLogDao auditLogDao = new AuditLogDaoImpl();

    // ------------------------------------------------------------------ queries

    public List<Hearing> findBetween(LocalDate from, LocalDate to, boolean includeCancelled) {
        return hearingDao.findBetween(from, to, includeCancelled);
    }

    public List<Hearing> forJudge(int judgeId, LocalDate from, LocalDate to) {
        return hearingDao.findForJudge(judgeId, from, to);
    }

    public List<Hearing> forLawyer(int lawyerId, LocalDate from, LocalDate to) {
        return hearingDao.findForLawyer(lawyerId, from, to);
    }

    public List<Hearing> byCase(int caseId) {
        return hearingDao.findByCase(caseId);
    }

    public Map<LocalDate, String> holidays() {
        return reportDao.holidays();
    }

    public List<LocalTime> allSlots() {
        return ScheduleRules.allSlots(
                AppConfig.getTime("hearing.day.start", LocalTime.of(10, 0)),
                AppConfig.getTime("hearing.day.end", LocalTime.of(16, 0)),
                AppConfig.getInt("hearing.slot.minutes", 30));
    }

    /** Free slots on a date for this judge, room and lawyer (empty if the court is closed). */
    public List<LocalTime> freeSlots(int judgeId, int roomId, int lawyerId, LocalDate date, Integer ignoreHearingId) {
        try {
            ScheduleRules.checkDay(date, LocalDate.now(), holidays());
        } catch (ScheduleConflictException closed) {
            return List.of();
        }
        List<LocalTime> slots = ScheduleRules.freeSlots(hearingDao.findByDate(date), judgeId, roomId, lawyerId,
                ignoreHearingId, allSlots());
        if (date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            slots = slots.stream().filter(t -> t.isAfter(now)).toList();
        }
        return slots;
    }

    /** All the checks for a new or moved hearing; throws a clear message on the first problem. */
    public void validateSlot(int judgeId, int roomId, int lawyerId, LocalDate date, LocalTime time,
                             Integer ignoreHearingId) throws ScheduleConflictException {
        ScheduleRules.checkDay(date, LocalDate.now(), holidays());
        if (time == null) {
            throw new ScheduleConflictException("Choose a time slot.");
        }
        if (!allSlots().contains(time)) {
            throw new ScheduleConflictException("Hearings can only start at the listed court slots.");
        }
        ScheduleRules.checkClash(hearingDao.findByDate(date), judgeId, roomId, lawyerId, time, ignoreHearingId,
                AppConfig.getInt("hearing.max.per.courtroom.per.day", 12));
    }

    // ------------------------------------------------------------------ scheduling

    /** Lists an active case for a further hearing in its judge's courtroom. */
    public void schedule(CourtCase c, LocalDate date, LocalTime time) throws ValidationException {
        if (!c.getStatus().isActive() || c.getStatus() == CaseStatus.RESERVED) {
            throw new ValidationException("Only registered or pending cases can be listed for a hearing.");
        }
        if (c.getJudgeId() == null || c.getCourtroomId() == null) {
            throw new ValidationException("Register the case and assign a judge first.");
        }
        validateSlot(c.getJudgeId(), c.getCourtroomId(), c.getFiledById(), date, time, null);

        DBConnection.inTransaction(con -> {
            hearingDao.insert(con, c.getId(), c.getJudgeId(), c.getCourtroomId(), date, time);
            if (c.getNextHearingDate() == null || date.isBefore(c.getNextHearingDate())
                    || c.getNextHearingDate().isBefore(LocalDate.now())) {
                caseDao.setNextHearing(con, c.getId(), date);
            }
            return null;
        });
        audit("HEARING_SCHEDULED", c.getCaseNumber() + " on " + date + " " + time);
    }

    public void reschedule(Hearing h, LocalDate date, LocalTime time) throws ValidationException {
        if (h.status() != HearingStatus.SCHEDULED) {
            throw new ValidationException("Only scheduled hearings can be moved.");
        }
        validateSlot(h.judgeId(), h.courtroomId(), h.lawyerId(), date, time, h.id());
        DBConnection.inTransaction(con -> {
            hearingDao.reschedule(con, h.id(), date, time);
            caseDao.setNextHearing(con, h.caseId(), date);
            return null;
        });
        audit("HEARING_RESCHEDULED", h.caseNumber() + " moved to " + date + " " + time);
    }

    public void cancel(Hearing h) throws ValidationException {
        if (h.status() != HearingStatus.SCHEDULED) {
            throw new ValidationException("This hearing is not scheduled, so it cannot be cancelled.");
        }
        DBConnection.inTransaction(con -> {
            hearingDao.updateStatus(con, h.id(), HearingStatus.CANCELLED);
            caseDao.setNextHearing(con, h.caseId(), null);
            return null;
        });
        // If another hearing is still scheduled, show that one as the next date
        hearingDao.findNextScheduled(h.caseId()).ifPresent(next ->
                DBConnection.inTransaction(con -> {
                    caseDao.setNextHearing(con, h.caseId(), next.date());
                    return null;
                }));
        audit("HEARING_CANCELLED", h.caseNumber() + " on " + h.date());
    }

    // ------------------------------------------------------------------ orders

    /**
     * Records the judge's order at a hearing and moves the case on, all in one transaction:
     * <ol>
     *     <li>insert the order</li>
     *     <li>mark the hearing completed</li>
     *     <li>update the case: list the next hearing, reserve judgment, or dispose of it</li>
     * </ol>
     */
    public void recordOrder(Hearing h, int judgeId, String text, OrderOutcome outcome,
                            LocalDate nextDate, LocalTime nextTime) throws ValidationException {
        String orderText = Validators.minLength(text, "Order", 10, 4000);
        if (outcome == null) {
            throw new ValidationException("Choose the outcome of the hearing.");
        }
        if (h.status() != HearingStatus.SCHEDULED) {
            throw new ValidationException("An order has already been recorded for this hearing.");
        }
        if (h.date().isAfter(LocalDate.now())) {
            throw new ValidationException("This hearing is on " + h.date()
                    + ". An order can only be recorded on or after the hearing date.");
        }
        CaseStatus target = outcome.getResultingStatus();
        if (h.caseStatus() != target && !h.caseStatus().canMoveTo(target)) {
            throw new InvalidStatusTransitionException(h.caseStatus(), target);
        }
        if (outcome.needsNextDate()) {
            if (nextDate == null || !nextDate.isAfter(h.date())) {
                throw new ValidationException("Choose a next hearing date after " + h.date() + ".");
            }
            validateSlot(h.judgeId(), h.courtroomId(), h.lawyerId(), nextDate, nextTime, null);
        }

        LocalDate today = LocalDate.now();
        DBConnection.inTransaction(con -> {
            orderDao.insert(con, h.caseId(), h.id(), judgeId, orderText, outcome,
                    outcome.needsNextDate() ? nextDate : null, today);
            hearingDao.updateStatus(con, h.id(), HearingStatus.COMPLETED);
            switch (outcome) {
                case DISPOSED -> {
                    caseDao.dispose(con, h.caseId(), orderText, today);
                    hearingDao.cancelScheduled(con, h.caseId());
                }
                case RESERVED -> {
                    caseDao.updateStatus(con, h.caseId(), CaseStatus.RESERVED, null);
                    hearingDao.cancelScheduled(con, h.caseId());
                }
                case ADJOURNED, NEXT_HEARING -> {
                    hearingDao.insert(con, h.caseId(), h.judgeId(), h.courtroomId(), nextDate, nextTime);
                    caseDao.updateStatus(con, h.caseId(), CaseStatus.PENDING, nextDate);
                }
            }
            return null;
        });
        LOG.info("Order recorded in " + h.caseNumber() + ": " + outcome);
        audit("ORDER_PASSED", h.caseNumber() + " - " + outcome.getDisplayName());
    }

    private void audit(String action, String details) {
        Integer by = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : null;
        auditLogDao.log(by, action, details);
    }
}
