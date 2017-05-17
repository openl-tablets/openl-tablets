package org.openl.rules.db.migration;

import org.flywaydb.core.Flyway;
import org.hibernate.dialect.*;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.db.utils.DBUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

public class DBMigrationBean {
    private static final String SCHEMA_SEPARATOR = ".";

    private final Logger log = LoggerFactory.getLogger(DBMigrationBean.class);

    @Autowired
    ServletContext servletContext;
    private String dbDriver;
    private String dbLogin;
    private String dbPassword;
    private String dbPrefix;
    private String dbUrl;
    private String dbSchema;
    private String dbUrlSeparator;
    private String additionalMigrationPaths;
    private DataSource dataSource;

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public String init() {
        String prefix = dbUrl.split(dbUrlSeparator)[0] + dbUrlSeparator;
        String url = dbUrl.split(dbUrlSeparator)[1];
        if (servletContext == null) {
            servletContext = FacesUtils.getServletContext();
        }
        DBUtils dbUtils = new DBUtils(servletContext);
        Connection dbConnection = dbUtils.createConnection(dbDriver, prefix, url, dbLogin, dbPassword);
        try {
            dbConnection.setAutoCommit(false);
            DatabaseMetaDataDialectResolutionInfoAdapter dialectResolutionInfo = new DatabaseMetaDataDialectResolutionInfoAdapter(dbConnection.getMetaData());
            Dialect dialect = new StandardDialectResolver().resolveDialect(dialectResolutionInfo);

            Flyway flyway = flywayInit(dialect);
            if (!dbUtils.isTableSchemaVersionExists(dbConnection)) {
                flyway.setInitVersion("0");
                flyway.init();
            }
            flyway.migrate();
            dbConnection.commit();
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            try {
                dbConnection.rollback();
            } catch (SQLException e1) {
                log.error(e.getMessage(), e);
            }
        } finally {
            try {
                dbConnection.close();
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        return "";
    }

    /**
     * Creates and initializes the Flyway metadata table.
     */
    private Flyway flywayInit(Dialect dialect) {
        // Set path to V1_Base_version.sql script
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);

        String schemaPrefix = StringUtils.isBlank(dbSchema) ? "" : StringUtils.trim(dbSchema) + SCHEMA_SEPARATOR;

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("schemaPrefix", schemaPrefix);
        placeholders.put("identity_column", getIdentityColumn(dialect));
        placeholders.put("create_hibernate_sequence", getCreateHibernateSequence(dialect));
        placeholders.put("bigint", dialect.getTypeName(Types.BIGINT));
        placeholders.put("longtext", dialect.getTypeName(Types.VARCHAR, 1000, 0, 0));
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(getScriptLocations(dialect));

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

    public String getAdditionalMigrationPaths() {
        return additionalMigrationPaths;
    }

    public void setAdditionalMigrationPaths(String additionalMigrationPaths) {
        this.additionalMigrationPaths = additionalMigrationPaths;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private String getIdentityColumn(Dialect dialect) {
        if (dialect.supportsIdentityColumns()) {
            String dataType = dialect.hasDataTypeInIdentityColumn() ? dialect.getTypeName(Types.BIGINT) : "";
            return dataType + " " + dialect.getIdentityColumnString(Types.BIGINT);
        } else {
            return dialect.getTypeName(Types.BIGINT) + " not null";
        }
    }

    private String getCreateHibernateSequence(Dialect dialect) {
        if (dialect.supportsIdentityColumns()) {
            return "";
        } else {
            String[] strings = dialect.getCreateSequenceStrings("hibernate_sequence", 1, 1);
            StringBuilder sb = new StringBuilder();
            for (String s : strings) {
                sb.append(s);
            }
            sb.append(";");
            return sb.toString();
        }
    }

    private String[] getScriptLocations(Dialect dialect) {
        List<String> locations = new ArrayList<String>();
        locations.add("db/migration/common");

        // DB-specific scripts can be added here:
        if (dialect instanceof Oracle8iDialect) {
            locations.add("db/migration/oracle");
        } else if (dialect instanceof MySQLDialect) {
            locations.add("db/migration/mysql");
        } else if (dialect instanceof SQLServerDialect) {
            locations.add("db/migration/mssqlserver");
        } else if (dialect instanceof H2Dialect) {
            locations.add("db/migration/h2");
        }

        // Additional migrations
        if (!StringUtils.isBlank(additionalMigrationPaths)) {
            Collections.addAll(locations, additionalMigrationPaths.trim().split("\\s*,\\s*"));
        }

        return locations.toArray(new String[locations.size()]);
    }

}
