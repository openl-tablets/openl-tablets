package org.openl.rules.db.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.faces.validator.ValidatorException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.config.ConfigurationManager;
import org.openl.rules.webstudio.web.install.InstallWizard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils {
    private static final String SQL_ERRORS_FILE_PATH = "db/sql-errors.properties";
    private static final String TABLE_FOR_VALIDATION = "schema_version";

    private final Logger log = LoggerFactory.getLogger(InstallWizard.class);

    private Map<String, Object> dbErrors;

    public DBUtils() {
        ConfigurationManager sqlErrorsConfig = new ConfigurationManager(false,
            null,
            System.getProperty("webapp.root") + "/WEB-INF/conf/" + SQL_ERRORS_FILE_PATH);
        dbErrors = sqlErrorsConfig.getProperties();
    }

    /**
     * Returns connection (session) to a specific database.
     *
     * @param dbDriver - database driver
     * @param dbPrefix - database prefix
     * @param dbUrl - database url
     * @param login - database login
     * @param password - database password
     * @return connection (session) to a specific database.
     */
    public Connection createConnection(String dbDriver, String dbPrefix, String dbUrl, String login, String password) {
        Connection conn;

        try {
            Class.forName(dbDriver);
            conn = DriverManager.getConnection((dbPrefix + dbUrl), login, password);
        } catch (ClassNotFoundException cnfe) {
            log.error(cnfe.getMessage(), cnfe);
            throw new ValidatorException(FacesUtils.createErrorMessage("Incorrect database driver"));
        } catch (SQLException sqle) {
            int errorCode = sqle.getErrorCode();
            String errorMessage = (String) dbErrors.get("" + errorCode);

            if (errorMessage != null) {
                log.error(sqle.getMessage(), sqle);
                throw new ValidatorException(FacesUtils.createErrorMessage(errorMessage));
            } else {
                log.error(sqle.getMessage(), sqle);
                throw new ValidatorException(
                    FacesUtils.createErrorMessage("Incorrect database URL, login or password"));
            }
        }

        return conn;
    }

    /**
     * Validates flyway table 'schema_version' exists or not.
     *
     * @param conn is a connection (session) with a specific database.
     * @return true if table 'schema_version' exists into DB
     */
    public boolean isTableSchemaVersionExists(Connection conn) throws SQLException {
        ResultSet rs = null;
        try {
            DatabaseMetaData meta = conn.getMetaData();
            if ("Oracle".equals(meta.getDatabaseProductName())) {
                rs = meta.getTables(null, meta.getUserName(), TABLE_FOR_VALIDATION, new String[] { "TABLE" });
            } else {
                rs = meta.getTables(null, null, TABLE_FOR_VALIDATION, new String[] { "TABLE" });
            }

            return rs.next();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            return false;
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }
}
