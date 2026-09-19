package org.openl.itest;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import org.openl.itest.core.JettyServer;

/**
 * Verifies that the current version can create the security schema on a supported RDBMS, and can upgrade
 * one first initialized by an older OpenL Studio release. Each concrete subclass covers one database
 * (MySQL, SQL Server, Oracle, PostgreSQL) by supplying its container, and every subclass is run once per
 * supported source.
 *
 * <p>A run is a single pass. An older release creates and seeds the schema when the source names one, then
 * the current version starts against the same database, applies its migrations and serves the suite.
 *
 * <p>The older releases tracked the schema with Flyway, the current one with Liquibase. The current
 * version must recognize the schema it is handed, adopt what is already there, and apply only what
 * that release was missing. On an empty database it creates the whole schema instead, which is where the
 * types of the change log reach a real database: a column type an upgraded schema already carries is never
 * written again.
 *
 * <p>The database backends are heavy, so each subclass carries {@link CiWithDocker}: the family runs
 * only on CI and skips on a local build.
 *
 * @author Yury Molchan
 */
abstract class AbstractRdbmsTest {

    /** The source that is no release at all: a database the current version finds empty. */
    private static final String EMPTY_DATABASE = "an empty database";

    /**
     * Databases the current version must be able to start against: an empty one, the oldest release an
     * upgrade starts from, and the release the change log is baselined at. Every later release left the same
     * schema as 6.0.0, apart from one column, which the H2 tests of the security module cover.
     */
    private static final List<String> SOURCES = List.of(EMPTY_DATABASE, "5.27.15", "6.0.0");

    /** Feeds {@link #migrate(String)}; JUnit resolves it through each concrete subclass. */
    static List<String> sources() {
        return SOURCES;
    }

    /** Docker host gateway, through which a container reaches a port published on the host. */
    private static final String HOST_GATEWAY = "host.docker.internal";

    /** Supplies the database container the current test migrates. */
    abstract JdbcDatabaseContainer<?> createContainer();

    @ParameterizedTest(name = "from {0}")
    @MethodSource("sources")
    void migrate(String source) throws Exception {
        try (var db = createContainer()) {
            db.start();

            // 1. Let the older OpenL Studio release create and seed the database, unless the run starts
            //    from an empty one.
            if (!EMPTY_DATABASE.equals(source)) {
                initWithRelease(db, source);
            }

            // 2. The current version must migrate that database and pass the suite.
            JettyServer.get()
                    .withInitParam("db.url", db.getJdbcUrl())
                    .withInitParam("db.user", db.getUsername())
                    .withInitParam("db.password", db.getPassword())
                    .test();
        }
    }

    /**
     * Runs the Studio image of the given release against the database until it has initialized the security
     * schema. The matching JDBC driver is mounted into {@code /opt/openl/lib}, because the image bundles no
     * third-party drivers.
     */
    private static void initWithRelease(JdbcDatabaseContainer<?> db, String release) throws Exception {
        var driverJar = Path.of(Class.forName(db.getDriverClassName())
                .getProtectionDomain().getCodeSource().getLocation().toURI());
        var image = DockerImageName.parse("openltablets/webstudio:" + release);
        try (GenericContainer<?> studio = new GenericContainer<>(image)) {
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
     * Asserts that the older release has created and seeded the security schema.
     *
     * <p>Flyway, the migration tool of those releases, creates its history table as a quoted, lower-case
     * identifier. The table name is therefore quoted with the database's own quote character; otherwise
     * Oracle would fold the unquoted name to upper case and fail to find it.
     */
    private static void assertSecurityDbInitialized(JdbcDatabaseContainer<?> db) throws Exception {
        try (var connection = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
                var statement = connection.createStatement()) {
            var quote = connection.getMetaData().getIdentifierQuoteString();
            var query = "SELECT COUNT(*) FROM " + quote + "openl_security_flyway" + quote;
            try (var rs = statement.executeQuery(query)) {
                rs.next();
                assertTrue(rs.getInt(1) > 0,
                        "The older OpenL Studio release must have initialized the security schema");
            }
        }
    }

}
