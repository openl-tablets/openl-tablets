package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.openl.itest.core.JettyServer;

/**
 * Verifies that the current version can upgrade a database first initialized by the previous OpenL
 * Studio release. One test per supported RDBMS (MySQL, SQL Server, Oracle, PostgreSQL).
 *
 * <p>Each test runs as a single pass. The previous-release image creates and seeds the schema, then
 * the current version starts against the same database, applies its migrations and serves the suite.
 *
 * <p>Between the two releases the migration scripts were only reformatted (tabs to spaces). That
 * changed their Flyway checksums without changing the SQL. The current version must tolerate the
 * checksum drift and still upgrade.
 */
@DisabledIfSystemProperty(named = "noDocker", matches = ".*")
class RdbmsTest {

    /**
     * The previous release whose database the current version must be able to upgrade. Its schema
     * and seed data are identical to the current ones; only the Flyway checksums of reformatted
     * migration scripts differ.
     */
    private static final String PREVIOUS_RELEASE_IMAGE = "openltablets/webstudio:6.0.0";

    /** Docker host gateway, through which a container reaches a port published on the host. */
    private static final String HOST_GATEWAY = "host.docker.internal";

    @Test
    void mysql() throws Exception {
        testDB(() -> new MySQLContainer("mysql:lts") {
            @Override
            public String getDriverClassName() {
                return "org.mariadb.jdbc.Driver";
            }
        });
    }

    /**
     * Note: <a href="https://learn.microsoft.com/en-us/azure/azure-sql-edge/features">Supported features of Azure SQL Edge</a>
     * 1. Azure SQL Edge will be retired on September 30, 2025.
     * 2. Azure SQL Edge no longer supports the ARM64 platform.
     */
    @Test
    void sqlserver() throws Exception {
        testDB(() -> new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest")
                .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server")).acceptLicense());
    }

    @Test
    void oracle() throws Exception {
        testDB(() -> new OracleContainer("gvenzl/oracle-free:slim-faststart"));
    }

    @Test
    void postgresql() throws Exception {
        testDB(() -> {
            var db = new PostgreSQLContainer("postgres:alpine");
            db.waitingFor(new HostPortWaitStrategy());
            return db;
        });
    }

    private static void testDB(Supplier<JdbcDatabaseContainer<?>> containerSupplier) throws Exception {
        try (var db = containerSupplier.get()) {
            db.start();

            // 1. Let the previous OpenL Studio release create and seed the database.
            initWithPreviousRelease(db);

            // 2. The current version must upgrade that database and pass the suite.
            JettyServer.get()
                    .withInitParam("db.url", db.getJdbcUrl())
                    .withInitParam("db.user", db.getUsername())
                    .withInitParam("db.password", db.getPassword())
                    .withProfile("acl")
                    .test();
        }
    }

    /**
     * Runs the previous-release Studio image against the given database until it has initialized the
     * security schema. The matching JDBC driver is mounted into {@code /opt/openl/lib}, because the
     * image bundles no third-party drivers.
     */
    private static void initWithPreviousRelease(JdbcDatabaseContainer<?> db) throws Exception {
        var driverJar = Path.of(Class.forName(db.getDriverClassName())
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        try (GenericContainer<?> studio = new GenericContainer<>(DockerImageName.parse(PREVIOUS_RELEASE_IMAGE))) {
            studio.withExtraHost(HOST_GATEWAY, "host-gateway");
            studio.withExposedPorts(8080);
            studio.withCopyFileToContainer(MountableFile.forHostPath(driverJar),
                    "/opt/openl/lib/" + driverJar.getFileName());
            studio.withEnv("user.mode", "multi");
            studio.withEnv("security.administrators", "admin");
            studio.withEnv("db.url", db.getJdbcUrl().replace(db.getHost() + ":", HOST_GATEWAY + ":"));
            studio.withEnv("db.user", db.getUsername());
            studio.withEnv("db.password", db.getPassword());
            studio.waitingFor(Wait.forHttp("/")
                    .forStatusCodeMatching(code -> code >= 200 && code < 500)
                    .withStartupTimeout(Duration.ofMinutes(5)));
            studio.start();
            assertSecurityDbInitialized(db);
        }
    }

    /**
     * Asserts that the previous-release image has created and seeded the security schema.
     *
     * <p>The migration tool creates its history table as a quoted, lower-case identifier. The table
     * name is therefore quoted with the database's own quote character; otherwise Oracle would fold
     * the unquoted name to upper case and fail to find it.
     */
    private static void assertSecurityDbInitialized(JdbcDatabaseContainer<?> db) throws Exception {
        try (var connection = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
                var statement = connection.createStatement()) {
            var quote = connection.getMetaData().getIdentifierQuoteString();
            var query = "SELECT COUNT(*) FROM " + quote + "openl_security_flyway" + quote;
            try (var rs = statement.executeQuery(query)) {
                rs.next();
                assertTrue(rs.getInt(1) > 0,
                        "The previous OpenL Studio release must have initialized the security schema");
            }
        }
    }

}
