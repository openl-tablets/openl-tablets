package org.openl.rules.db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.dialect.internal.StandardDialectResolver;
import org.hibernate.engine.jdbc.dialect.spi.DatabaseMetaDataDialectResolutionInfoAdapter;

public class DBMigrationBean {
    private DataSource dataSource;

    public void init() throws Exception {
        Connection connection = dataSource.getConnection();
        Dialect dialect;
        String databaseCode;
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseCode = metaData.getDatabaseProductName().toLowerCase().replace(" ", "_");
            DatabaseMetaDataDialectResolutionInfoAdapter dialectResolutionInfo = new DatabaseMetaDataDialectResolutionInfoAdapter(
                connection.getMetaData());
            dialect = new StandardDialectResolver().resolveDialect(dialectResolutionInfo);
        } finally {
            connection.close();
        }
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

        flyway.setLocations("db/migration/common", "db/migration/" + databaseCode);

        flyway.setBaselineVersionAsString("0");
        flyway.setBaselineOnMigrate(true);
        flyway.migrate();
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
}
