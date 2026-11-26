package com.court.service;

import com.court.dao.AuditLogDao;
import com.court.dao.UserDao;
import com.court.dao.impl.AuditLogDaoImpl;
import com.court.dao.impl.UserDaoImpl;
import com.court.exception.AuthenticationException;
import com.court.model.User;
import com.court.util.PasswordUtil;

import java.util.logging.Logger;

/**
 * Login and logout logic. Controllers call this class - they never touch the DAO directly.
 */
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class.getName());

    private final UserDao userDao;
    private final AuditLogDao auditLogDao;

    public AuthService() {
        this(new UserDaoImpl(), new AuditLogDaoImpl());
    }

    /** Constructor injection - lets unit tests pass fake DAOs. */
    public AuthService(UserDao userDao, AuditLogDao auditLogDao) {
        this.userDao = userDao;
        this.auditLogDao = auditLogDao;
    }

    /**
     * Checks the username and password.
     *
     * @return the logged-in user (a Registrar, Judge or Lawyer object)
     * @throws AuthenticationException if the details are wrong or the account is inactive
     */
    public User login(String username, String password) throws AuthenticationException {
        if (username == null || username.isBlank() || password == null || password.isEmpty()) {
            throw new AuthenticationException("Please enter both username and password.");
        }

        // Optional: findByUsername may return nothing; filter keeps the user only if the password matches
        User user = userDao.findByUsername(username)
                .filter(u -> PasswordUtil.verify(password, u.getPasswordHash()))
                .orElseThrow(() -> {
                    LOG.warning("Failed login attempt for username '" + username + "'");
                    return new AuthenticationException("Invalid username or password.");
                });

        if (!user.isActive()) {
            LOG.warning("Login blocked for deactivated account '" + username + "'");
            throw new AuthenticationException("Your account has been deactivated. Please contact the Registrar.");
        }

        auditLogDao.log(user.getId(), "LOGIN", user.getRole() + " logged in");
        LOG.info("Login successful: " + user.getUsername() + " (" + user.getRole() + ")");
        return user;
    }

    public void logout(User user) {
        auditLogDao.log(user.getId(), "LOGOUT", user.getRole() + " logged out");
        LOG.info("Logout: " + user.getUsername());
    }
}
