package com.court.service;

import com.court.dao.AuditLogDao;
import com.court.dao.UserDao;
import com.court.dao.impl.AuditLogDaoImpl;
import com.court.dao.impl.UserDaoImpl;
import com.court.exception.DataAccessException;
import com.court.exception.ValidationException;
import com.court.model.Judge;
import com.court.model.Lawyer;
import com.court.model.Role;
import com.court.model.User;
import com.court.util.PasswordUtil;
import com.court.util.SessionManager;
import com.court.util.Validators;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Managing judge and lawyer accounts (Registrar only).
 */
public class UserService {

    private static final Logger LOG = Logger.getLogger(UserService.class.getName());

    /** Password given to lawyers imported from a CSV file when the file has none. */
    public static final String DEFAULT_IMPORT_PASSWORD = "welcome123";

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;

    public UserService() {
        this(new UserDaoImpl(), new AuditLogDaoImpl());
    }

    public UserService(UserDao userDao, AuditLogDao auditLogDao) {
        this.userDao = userDao;
        this.auditLogDao = auditLogDao;
    }

    /** Users of one role, sorted by name with a Comparator. */
    public List<User> findByRole(Role role) {
        List<User> users = new ArrayList<>(userDao.findByRole(role));
        users.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return users;
    }

    /** Active judges who sit in a courtroom - the only ones a case can be assigned to. */
    public List<Judge> assignableJudges() {
        return userDao.findByRole(Role.JUDGE).stream()
                .filter(User::isActive)
                .map(u -> (Judge) u)
                .filter(Judge::hasCourtroom)
                .sorted(Comparator.comparing(Judge::getCourtroomName))
                .toList();
    }

    /**
     * Validates and saves a new or edited judge/lawyer.
     *
     * @param password required for a new account, ignored when editing
     */
    public User save(User user, String password) throws ValidationException {
        user.setName(Validators.required(user.getName(), "Name", 100));
        user.setEmail(Validators.email(user.getEmail()));
        user.setPhone(Validators.phone(user.getPhone()));

        if (user instanceof Judge judge) {
            judge.setDesignation(Validators.required(judge.getDesignation(), "Designation", 100));
        } else if (user instanceof Lawyer lawyer) {
            lawyer.setBarId(Validators.barId(lawyer.getBarId()));
        }

        boolean isNew = user.getId() == 0;
        try {
            if (isNew) {
                user.setUsername(Validators.username(user.getUsername()));
                if (userDao.usernameExists(user.getUsername())) {
                    throw new ValidationException("The username \"" + user.getUsername() + "\" is already taken.");
                }
                user.setPasswordHash(PasswordUtil.hash(Validators.password(password)));
                userDao.save(user);
            } else {
                userDao.update(user);
            }
        } catch (DataAccessException e) {
            // e.g. the same Bar ID or courtroom is already used by someone else
            throw new ValidationException(e.getMessage());
        }
        audit(isNew ? "USER_CREATED" : "USER_UPDATED", user.getRole() + " " + user.getUsername());
        return user;
    }

    public void setActive(User user, boolean active) {
        user.setActive(active);
        userDao.update(user);
        audit(active ? "USER_ACTIVATED" : "USER_DEACTIVATED", user.getUsername());
    }

    public void resetPassword(User user, String newPassword) throws ValidationException {
        userDao.updatePassword(user.getId(), PasswordUtil.hash(Validators.password(newPassword)));
        audit("PASSWORD_RESET", user.getUsername());
    }

    public void delete(User user) throws ValidationException {
        try {
            userDao.delete(user.getId());
        } catch (DataAccessException e) {
            throw new ValidationException(e.getMessage());
        }
        audit("USER_DELETED", user.getUsername());
    }

    // ------------------------------------------------------------------ CSV import (character stream)

    /** How a CSV import went: how many lawyers were added and what went wrong on which line. */
    public record ImportResult(int imported, List<String> problems) {
    }

    /**
     * Imports lawyers from a CSV file with the header
     * {@code name,username,bar_id,email,phone[,password]}.
     * Lines with problems are skipped and reported; the rest are imported.
     */
    public ImportResult importLawyers(Path csvFile) throws IOException {
        /* A line of the file together with its line number, so problems can name the line. */
        record CsvLine(int number, String text) { }

        List<CsvLine> lines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(csvFile, StandardCharsets.UTF_8)) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                lines.add(new CsvLine(++number, line));
            }
        }

        // Iterator lets us remove blank lines and the header while looping
        Iterator<CsvLine> it = lines.iterator();
        while (it.hasNext()) {
            String text = it.next().text().trim();
            if (text.isEmpty() || text.toLowerCase().startsWith("name,")) {
                it.remove();
            }
        }

        int imported = 0;
        List<String> problems = new ArrayList<>();
        Set<String> seenUsernames = new HashSet<>();
        for (CsvLine csvLine : lines) {
            int lineNo = csvLine.number();
            String line = csvLine.text();
            String[] f = line.split(",", -1);
            if (f.length < 5) {
                problems.add("Line " + lineNo + ": expected 5 values (name, username, bar_id, email, phone).");
                continue;
            }
            Lawyer lawyer = new Lawyer();
            try {
                lawyer.setName(f[0].trim());
                lawyer.setUsername(f[1].trim().isEmpty() ? "x" : f[1].trim());
                lawyer.setBarId(f[2].trim());
                lawyer.setEmail(f[3].trim());
                lawyer.setPhone(f[4].trim());
                if (!seenUsernames.add(lawyer.getUsername())) {
                    throw new ValidationException("username " + lawyer.getUsername() + " appears twice in the file.");
                }
                String password = f.length > 5 && !f[5].isBlank() ? f[5].trim() : DEFAULT_IMPORT_PASSWORD;
                save(lawyer, password);
                imported++;
            } catch (ValidationException e) {
                problems.add("Line " + lineNo + " (" + f[0].trim() + "): " + e.getMessage());
            }
        }
        LOG.info("CSV import from " + csvFile.getFileName() + ": " + imported + " imported, "
                + problems.size() + " skipped");
        return new ImportResult(imported, problems);
    }

    private void audit(String action, String details) {
        Integer by = SessionManager.isLoggedIn() ? SessionManager.getCurrentUser().getId() : null;
        auditLogDao.log(by, action, details);
    }
}
