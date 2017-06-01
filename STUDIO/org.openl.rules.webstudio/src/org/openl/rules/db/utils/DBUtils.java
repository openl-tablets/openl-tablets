package org.openl.rules.db.utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import javax.faces.validator.ValidatorException;
import javax.servlet.ServletContext;

import org.openl.commons.web.jsf.FacesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DBUtils {
    private static final String SQL_ERRORS_FILE_PATH = "/WEB-INF/conf/db/sql-errors.properties";

    private final Logger log = LoggerFactory.getLogger(DBUtils.class);

    private Map<Object, Object> dbErrors;

    public DBUtils(ServletContext servletContext) {
        Properties properties = new Properties();
        try {
            properties.load(servletContext.getResourceAsStream(SQL_ERRORS_FILE_PATH));
        } catch (IOException e) {
            log.error("Cannot to load {} file.", SQL_ERRORS_FILE_PATH, e);
        }

        dbErrors = properties;
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
}
