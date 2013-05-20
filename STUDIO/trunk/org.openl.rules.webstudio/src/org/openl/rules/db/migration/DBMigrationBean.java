package org.openl.rules.db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.db.utils.DBUtils;
import org.openl.util.Log;

import com.googlecode.flyway.core.Flyway;

@ManagedBean
@SessionScoped
public class DBMigrationBean  {
    private String dbDriver;
    private String dbLogin;
    private String dbPassword;
    private String dbPrefix;
    private String dbUrl;
    private String dbSchema;
    private String dbUrlSeparator;
    private Connection dbConnection;
    private String sqlScriptPath = System.getProperty("webapp.root") + "db/mysql_changes/Modifyng_mysql_DB.sql";
    private DataSource dataSource;
    
    public String init() {
        String prefix = dbUrl.split(dbUrlSeparator)[0] + dbUrlSeparator;
        String url = dbUrl.split(dbUrlSeparator)[1];
        DBUtils dbUtils = new DBUtils();
System.out.println(dbDriver + "\n" +  prefix + "\n" +  dbUrl + "\n" +  dbLogin + "\n" +  dbPassword);        
        dbConnection = dbUtils.createConnection(dbDriver, prefix, url, dbLogin, dbPassword);
        Statement st;
        try {
            st = dbConnection.createStatement();
            DatabaseMetaData meta = dbConnection.getMetaData();
            String dbVendor = meta.getDatabaseProductName();

            if (StringUtils.containsIgnoreCase(dbVendor, "MySQL")) {
                if (dbUtils.isDatabaseExists(dbConnection) & !dbUtils.isTableSchemaVersionIxists(dbConnection)) {
                    dbUtils.executeSQL(sqlScriptPath, st);
                    flywayInit().init();
                }

            } else {
                flywayInit().migrate();
            }
        } catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        return "";
    }
    
    /**
     * Creates and initializes the Flyway metadata table.
     */
    public Flyway flywayInit() {
        // Set path to V1_Base_version.sql script
        Flyway flyway = new Flyway();
        System.out.println("filesystem:" + System.getProperty("webapp.root") + "db");
        flyway.setDataSource(dataSource);
        System.out.println(flyway.getDataSource().toString());
        //flyway.setLocations("filesystem:" + System.getProperty("webapp.root") + "db");
        //System.out.println(flyway.getLocations());
        return flyway;
    }

    public String getDbDriver() {
        return dbDriver;
    }

    public void setDbDriver(String dbDriver) {
        this.dbDriver = dbDriver;
    }

    public String getDbLogin() {
        return dbLogin;
    }

    public void setDbLogin(String dbLogin) {
        this.dbLogin = dbLogin;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getDbPrefix() {
        return dbPrefix;
    }

    public void setDbPrefix(String dbPrefix) {
        this.dbPrefix = dbPrefix;
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public void setDbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public String getDbSchema() {
        return dbSchema;
    }

    public void setDbSchema(String dbSchema) {
        this.dbSchema = dbSchema;
    }

    public String getDbUrlSeparator() {
        return dbUrlSeparator;
    }

    public void setDbUrlSeparator(String dbUrlSeparator) {
        this.dbUrlSeparator = dbUrlSeparator;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
}
