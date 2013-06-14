package org.openl.rules.db.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.validator.ValidatorException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.config.ConfigurationManager;
import org.openl.rules.webstudio.web.install.InstallWizard;

public class DBUtils {
    private Map<String, Object> dbErrors;
    private ConfigurationManager sqlErrorsConfig;
    private String sqlErrorsFilePath = "db/sql-errors.properties";
    private String sqlScriptPath = System.getProperty("webapp.root") + "db/mysql_changes/Modifyng_mysql_DB.sql";
    private final Log LOG = LogFactory.getLog(InstallWizard.class);
    private Connection dbConnection;
    private  String tableForValidation = "schema_version";

    public DBUtils() {
        sqlErrorsConfig = new ConfigurationManager(false, null, System.getProperty("webapp.root") + "/WEB-INF/conf/" + sqlErrorsFilePath);
        dbErrors = sqlErrorsConfig.getProperties();
    }

/*    public boolean init(String dbDriver, String dbPrefix, String dbUrl, String login, String password) {
        dbConnection = createConnection(dbDriver, dbPrefix, dbUrl, login, password);
        Statement st;
        boolean isModified = false;
        try {
            st = dbConnection.createStatement();
            DatabaseMetaData meta = dbConnection.getMetaData();
            String dbVendor = meta.getDatabaseProductName();

            if (StringUtils.containsIgnoreCase(dbVendor, "MySQL")) {
                if (isDatabaseExists(dbConnection) & !isTableSchemaVersionIxists(dbConnection)) {
                        executeSQL(sqlScriptPath, st);
                }
                isModified = true;
            }  
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        

        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                LOG.error(e.getMessage(), e);
            }
        }
        
        return isModified;
    }*/

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
        Connection conn = null;
        int errorCode = 0;

        try {
            Class.forName(dbDriver);
            conn = DriverManager.getConnection((dbPrefix + dbUrl), login, password);
        } catch (ClassNotFoundException cnfe) {
            LOG.error(cnfe.getMessage(), cnfe);
            throw new ValidatorException(new FacesMessage("Incorrectd database driver"));
        } catch (SQLException sqle) {
            errorCode = sqle.getErrorCode();
            String errorMessage = (String) dbErrors.get("" + errorCode);

            if (errorMessage != null) {
                LOG.error(sqle.getMessage(), sqle);
                throw new ValidatorException(new FacesMessage(errorMessage));
            } else {
                LOG.error(sqle.getMessage(), sqle);
                throw new ValidatorException(new FacesMessage("Incorrect database URL, login or password"));
            }
        }

        return conn;
    }

    /**
     * Validates database exists or not.
     * 
     * @param conn is a connection (session) with a specific database.
     * @return true if database exists
     */
    public boolean isDatabaseExists(Connection conn) {
        String schemaName = "";
        try {
            schemaName = conn.getCatalog();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        if (getDbSchemaList(conn).contains(schemaName)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Validates flyway table 'schema_version' exists or not.
     * 
     * @param conn is a connection (session) with a specific database.
     * @return true if table 'schema_version' exists into DB
     */
    public boolean isTableSchemaVersionIxists(Connection conn) throws SQLException {

        if (getDBOpenlTables(conn).contains(tableForValidation)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns a list of schemas form OpenL database
     * 
     * @param conn is a connection (session) with a specific database.
     * @return a list of database schemas
     */
    private List<String> getDbSchemaList(Connection conn) {
        List<String> dbSchemasList = new ArrayList<String>();
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getCatalogs();

            while (rs.next()) {
                String databaseName = rs.getString("TABLE_CAT");
                dbSchemasList.add(databaseName);
            }

        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return dbSchemasList;
    }

    /**
     * Returns a list of tables from OpenL database
     * 
     * @param conn is a connection (session) with a specific database.
     * @return a list of tables from OpenL database
     */
    private List<String> getDBOpenlTables(Connection conn) {
        List<String> dbOpenlTablesList = new ArrayList<String>();

        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getTables(null, null, "%", null);

            while (rs.next()) {
                String dbTableName = rs.getString("TABLE_NAME");
                dbOpenlTablesList.add(dbTableName);
            }
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
        return dbOpenlTablesList;
    }

    /**
     * Executes SQL script for changing columns and colums datatypes into
     * existing MySQL database
     * 
     * @param sqlFilePath a path to SQL script
     * @param st - Statement. The object used for executing a static SQL
     *            statement and returning the results it produces.
     */
    public void executeSQL(String sqlFilePath, Statement st) {
        try {
            BufferedReader buffReader = new BufferedReader(new FileReader(sqlFilePath));
            StringBuffer sb = new StringBuffer();
            String str;

            while ((str = buffReader.readLine()) != null) {
                sb.append(str);
                if (str.isEmpty()) {
                    st.addBatch(sb.toString());
                    // clear the Stringbuffer content
                    sb.delete(0, sb.length());
                }
            }

            buffReader.close();
            st.executeBatch();
            st.close();

        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

}
