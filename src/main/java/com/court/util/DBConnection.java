package com.court.util;

import com.court.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Gives out JDBC connections to the MySQL database.
 * <p>
 * JDBC architecture in this project:
 * <pre>
 *   DAO class  --&gt;  JDBC API (java.sql)  --&gt;  DriverManager  --&gt;  MySQL Connector/J driver  --&gt;  MySQL
 * </pre>
 * Each DAO method asks for a connection and closes it with try-with-resources,
 * so no connection is ever left open.
 */
public final class DBConnection {

    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = AppConfig.get("db.url");
        String user = AppConfig.get("db.user");
        String password = AppConfig.get("db.password");
        if (url == null) {
            throw new SQLException("db.url is missing in db.properties");
        }
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Opens and closes one connection to check that the database is reachable.
     * Called once at startup from {@code Main.init()}.
     */
    public static void testConnection() {
        try (Connection con = getConnection()) {
            LOG.info("Connected to database: " + con.getMetaData().getDatabaseProductName()
                    + " " + con.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            throw new DataAccessException("Cannot connect to the database. Check that MySQL is running "
                    + "and the settings in db.properties are correct.\n\nDetails: " + e.getMessage(), e);
        }
    }

    /** Rolls back quietly - used inside catch blocks of transactions. */
    public static void rollbackQuietly(Connection con) {
        if (con == null) return;
        try {
            con.rollback();
        } catch (SQLException e) {
            LOG.warning("Rollback failed: " + e.getMessage());
        }
    }
}
