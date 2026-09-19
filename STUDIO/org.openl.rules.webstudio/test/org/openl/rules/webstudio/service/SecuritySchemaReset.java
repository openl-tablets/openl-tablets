package org.openl.rules.webstudio.service;

import java.sql.SQLException;
import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.RequiredArgsConstructor;

/**
 * Rebuilds the security schema of a test database from the change log.
 *
 * <p>Every test starts from the schema and the seed data an empty installation gets, so the users, groups and
 * permissions one test creates never reach the next one.
 */
@RequiredArgsConstructor
public class SecuritySchemaReset {

    private static final String CHANGE_LOG = "db/changelog/db.changelog-master.xml";

    private final DataSource dataSource;

    public void reset() throws SQLException, LiquibaseException {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            try (var liquibase = new Liquibase(CHANGE_LOG, new ClassLoaderResourceAccessor(), database)) {
                liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
                liquibase.dropAll();
                liquibase.update(new Contexts(), new LabelExpression());
            }
        }
    }
}
