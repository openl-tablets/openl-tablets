package org.openl.rules.db.migration;

import java.sql.Connection;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;
import org.openl.util.StringUtils;

public class DBMigrationBean {
    private String additionalMigrationPaths;
    private DataSource dataSource;

    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        Dialect dialect;
        try {
            DatabaseMetaDataDialectResolutionInfoAdapter dialectResolutionInfo = new DatabaseMetaDataDialectResolutionInfoAdapter(
                connection.getMetaData());
            dialect = new StandardDialectResolver().resolveDialect(dialectResolutionInfo);
        } finally {
            connection.close();
        }
        Flyway flyway = flywayInit(dialect);
        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
    }

    /**
     * Creates and initializes the Flyway metadata table.
     */
    private Flyway flywayInit(Dialect dialect) {
        // Set path to V1_Base_version.sql script
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("schemaPrefix", "");
        placeholders.put("identity_column", getIdentityColumn(dialect));
        placeholders.put("create_hibernate_sequence", "");
        placeholders.put("bigint", dialect.getTypeName(Types.BIGINT));
        placeholders.put("longtext", dialect.getTypeName(Types.VARCHAR, 1000, 0, 0));
        flyway.setPlaceholders(placeholders);

        flyway.setLocations(getScriptLocations(dialect));

        return flyway;
    }

    public String getAdditionalMigrationPaths() {
        return additionalMigrationPaths;
    }

    public void setAdditionalMigrationPaths(String additionalMigrationPaths) {
        this.additionalMigrationPaths = additionalMigrationPaths;
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
        } else if (dialect instanceof PostgreSQL81Dialect) {
            locations.add("db/migration/postgresql");
        } else {
            locations.add("db/migration/other");
        }

        // Additional migrations
        if (StringUtils.isNotBlank(additionalMigrationPaths)) {
            String[] split = StringUtils.split(additionalMigrationPaths, ',');
            locations.addAll(Arrays.asList(split));
        }

        return locations.toArray(new String[locations.size()]);
    }
}
