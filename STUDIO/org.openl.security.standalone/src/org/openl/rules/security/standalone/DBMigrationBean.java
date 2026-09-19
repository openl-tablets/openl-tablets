package org.openl.rules.security.standalone;

import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.Scope;
import liquibase.UpdateSummaryOutputEnum;
import liquibase.analytics.configuration.AnalyticsArgs;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.ui.LoggerUIService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Brings the security database up to the schema this release expects.
 *
 * <p>The change log is baselined at the schema of OpenL Tablets 6.0.0: an empty database gets it created in one
 * step, together with the default groups and the default permissions. A database handed over by 5.27.15 is
 * converted to that schema, and one handed over by 6.0.0 or later is adopted as it is; either way only the
 * changes it is missing are applied.
 *
 * <p>A database of a release older than 5.27.10 is refused: the migration stops, the database is left
 * untouched, and the failure names the release to upgrade through.
 *
 * <p>Each change is described once and rendered for the database in use, so one change log serves H2, MySQL,
 * MariaDB, SQL Server, Azure SQL Database, Oracle and PostgreSQL.
 */
@Slf4j
public class DBMigrationBean {

    /** Change log listing every schema version, applied in order. */
    private static final String CHANGE_LOG = "db/changelog/db.changelog-master.xml";

    @Setter
    private DataSource dataSource;

    public void init() throws SQLException, LiquibaseException {
        try (var connection = dataSource.getConnection()) {
            var database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            log.info("Migrating the security schema of {}.", database.getDatabaseProductName());
            migrate(database);
        }
    }

    /**
     * Applies the change log to the given database.
     *
     * <p>The migration reports its progress to the application log rather than to the console, and sends no
     * usage statistics: an installation is not expected to reach the network of the migration tool.
     */
    private static void migrate(Database database) throws LiquibaseException {
        var settings = Map.<String, Object>of(AnalyticsArgs.ENABLED.getKey(), Boolean.FALSE,
                Scope.Attr.ui.name(), new LoggerUIService());
        try (var liquibase = new Liquibase(CHANGE_LOG, new ClassLoaderResourceAccessor(), database)) {
            liquibase.setShowSummaryOutput(UpdateSummaryOutputEnum.LOG);
            // Scope.child() declares the broadest exception, because it runs arbitrary code.
            Scope.child(settings, () -> liquibase.update(new Contexts(), new LabelExpression()));
        } catch (Exception e) {
            throw new LiquibaseException("Failed to migrate the security schema.", e);
        }
    }
}
