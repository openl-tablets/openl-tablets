package org.openl.rules.security.standalone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import liquibase.exception.LiquibaseException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Verifies the migration of the security schema on H2, the database an installation uses unless another one is
 * configured.
 *
 * <p>Each script under {@code db/} reproduces what one release left behind, layered on the one before it, so a
 * test can start from a database handed over by any supported release.
 *
 * @author Yury Molchan
 */
class DBMigrationBeanTest {

    private static final String RELEASE_5_27_15 = "db/openl-5.27.15-h2.sql";
    private static final String RELEASE_6_0_0 = "db/openl-6.0.0-h2.sql";
    private static final String RELEASE_6_4_0 = "db/openl-6.4.0-h2.sql";

    /**
     * Permissions of the default groups on the three repository roots, as every supported release has to end
     * up with them. Mask 15 grants view, edit, create and delete; mask 1 grants view alone.
     */
    private static final List<String> DEFAULT_PERMISSIONS = List.of(
            "1|Analysts|15", "1|Deployers|1", "1|Developers|15", "1|Testers|1", "1|Viewers|1",
            "2|Analysts|1", "2|Deployers|15", "2|Developers|1", "2|Testers|1", "2|Viewers|1",
            "3|Analysts|1", "3|Deployers|15", "3|Developers|1", "3|Testers|1", "3|Viewers|1");

    private DataSource dataSource;

    static List<Arguments> supportedReleases() {
        return List.of(
                Arguments.of("5.27.15", new String[] {RELEASE_5_27_15}),
                Arguments.of("6.0.0", new String[] {RELEASE_5_27_15, RELEASE_6_0_0}),
                Arguments.of("6.4.0", new String[] {RELEASE_5_27_15, RELEASE_6_0_0, RELEASE_6_4_0}));
    }

    @BeforeEach
    void freshDatabase() {
        var h2 = new JdbcDataSource();
        h2.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource = h2;
    }

    @Test
    void createsSchemaOfEmptyDatabase() throws Exception {
        migrate();

        assertEquals(List.of("Administrators", "Analysts", "Deployers", "Developers", "Testers", "Viewers"),
                query("SELECT groupName FROM OpenL_Groups ORDER BY groupName"));
        assertEquals(List.of("Administrators|ADMIN"), authorities());
        assertEquals(DEFAULT_PERMISSIONS, permissions());
        assertEquals(List.of("3"), query("SELECT COUNT(*) FROM acl_object_identity WHERE owner_sid ="
                + " (SELECT id FROM acl_sid WHERE sid = 'ADMIN')"));
        assertEquals(List.of("0"), query("SELECT COUNT(*) FROM OpenL_Users"));
        assertEquals(List.of("0"), query("SELECT COUNT(*) FROM OpenL_PAT_Tokens"));
        // The tables that attached a tag to a project belong to the releases before 6.0.0.
        assertFalse(tableExists("OPENL_PROJECTS"));
        assertFalse(tableExists("OPENL_PROJECT_TAGS"));
    }

    /**
     * The columns the product fills itself hold ASCII alone, and the ones whose values all have the same
     * length are fixed. H2 reports a fixed-length column as CHARACTER and a variable-length one as CHARACTER
     * VARYING.
     */
    @Test
    void declaresFixedLengthColumnsForTheValuesOfOneLength() throws Exception {
        migrate();

        assertEquals(List.of("CHARACTER|36"), columnType("OPENL_LOCK", "LOCK_KEY"));
        assertEquals(List.of("CHARACTER|36"), columnType("OPENL_LOCK", "CLIENT_ID"));
        assertEquals(List.of("CHARACTER VARYING|100"), columnType("OPENL_LOCK", "REGION"));
        assertEquals(List.of("CHARACTER|16"), columnType("OPENL_PAT_TOKENS", "PUBLICID"));
        // A name a person typed keeps the variable length it always had.
        assertEquals(List.of("CHARACTER VARYING|100"), columnType("OPENL_PAT_TOKENS", "NAME"));
    }

    /**
     * A foreign key column carries the type of the column it references. Oracle refuses a foreign key between
     * a national and a non-national column, so every login name is declared through the same property.
     */
    @Test
    void keepsForeignKeyColumnsOfTheTypeTheyReference() throws Exception {
        migrate();

        var referenced = columnType("OPENL_USERS", "LOGINNAME");
        for (var table : List.of("OPENL_USER2GROUP", "OPENL_USERSETTINGS", "OPENL_EXTERNAL_GROUPS",
                "OPENL_PAT_TOKENS")) {
            assertEquals(referenced, columnType(table, "LOGINNAME"), table + " references OpenL_Users");
        }
    }

    @ParameterizedTest(name = "from {0}")
    @MethodSource("supportedReleases")
    void upgradesDatabaseOfEverySupportedRelease(String release, String[] scripts) throws Exception {
        runScripts(scripts);

        migrate();

        // Whichever release handed the database over, the permissions it ends up with are the same.
        assertEquals(DEFAULT_PERMISSIONS, permissions(), "permissions after upgrading from " + release);
        assertEquals(List.of("Administrators|ADMIN"), authorities());
        // Everything the releases after the source added is in place.
        assertEquals(List.of("0"), query("SELECT COUNT(*) FROM OpenL_PAT_Tokens"));
        assertEquals(List.of("0"), query("SELECT COUNT(*) FROM OpenL_Users WHERE lastLogin IS NOT NULL"));
        assertTrue(indexExists("IDX_OPENL_EXTERNAL_GROUPS_GROUPNAME"));
        // The history of the previous migration tool is gone.
        assertFalse(tableExists("openl_security_flyway"));
        assertTrue(tableExists("DATABASECHANGELOG"));
        // The tables that attached a tag to a project are left as they are: the application moves the tags
        // into the projects and drops them once every project has its own file.
        assertTrue(tableExists("OPENL_PROJECTS"), "the legacy tag tables are the migration's own to drop");
        assertTrue(tableExists("OPENL_PROJECT_TAGS"));
    }

    @Test
    void keepsWhatAnUpgradedDatabaseAlreadyHeld() throws Exception {
        runScripts(RELEASE_5_27_15);
        execute("INSERT INTO OpenL_Users (loginName, displayName) VALUES ('jdoe', 'John Doe')");
        execute("DELETE FROM OpenL_Groups WHERE groupName = 'Analysts'");

        migrate();

        assertEquals(List.of("jdoe"), query("SELECT loginName FROM OpenL_Users"));
        assertEquals(List.of("5"), query("SELECT COUNT(*) FROM OpenL_Groups"));
    }

    @ParameterizedTest(name = "from {0}")
    @MethodSource("supportedReleases")
    void seedsNothingIntoADatabaseAnAdministratorHasEmptied(String release, String[] scripts) throws Exception {
        runScripts(scripts);
        // An installation that authenticates through an external provider may hold no local group at all, and
        // the groups screen lets an administrator delete the last one.
        execute("DELETE FROM OpenL_Group_Authorities");
        execute("DELETE FROM OpenL_User2Group");
        execute("DELETE FROM OpenL_Groups");

        migrate();

        assertEquals(List.of(), query("SELECT groupName FROM OpenL_Groups"),
                "groups deleted by an administrator must not come back, upgrading from " + release);
        assertEquals(List.of(), authorities());
        // The permissions the installation already had are still its own, not the defaults again.
        assertEquals(DEFAULT_PERMISSIONS, permissions());
    }

    @Test
    void refusesDatabaseOlderThanTheOldestSupportedRelease() throws Exception {
        runScripts(RELEASE_5_27_15);
        // Before 5.27.10 the group hierarchy lived in a table of its own, which that release flattened into
        // the group memberships and then dropped.
        execute("CREATE TABLE OpenL_Group2Group (groupID BIGINT NOT NULL, includedGroupID BIGINT NOT NULL,"
                + " PRIMARY KEY (groupID, includedGroupID))");
        var before = tables();

        var refusal = assertThrows(LiquibaseException.class, this::migrate);

        assertTrue(causeMessages(refusal).contains("5.27.10"),
                "The refusal must name the release to upgrade through: " + refusal);
        // Nothing is converted, no change set is recorded, and the history of the previous migration tool is
        // kept for the administrator.
        assertEquals(before, tables());
        assertEquals(List.of("0"), query("SELECT COUNT(*) FROM DATABASECHANGELOG"));
        assertTrue(tableExists("openl_security_flyway"));
    }

    @Test
    void leavesAnAlreadyMigratedDatabaseAlone() throws Exception {
        migrate();
        execute("INSERT INTO OpenL_Users (loginName, displayName) VALUES ('jdoe', 'John Doe')");

        migrate();

        assertEquals(List.of("jdoe"), query("SELECT loginName FROM OpenL_Users"));
        assertEquals(List.of("6"), query("SELECT COUNT(*) FROM OpenL_Groups"));
        assertEquals(DEFAULT_PERMISSIONS, permissions());
    }

    private void migrate() throws Exception {
        var migration = new DBMigrationBean();
        migration.setDataSource(dataSource);
        migration.init();
    }

    /** Permissions as "repository root | group | mask", which holds across databases and surrogate keys. */
    private List<String> permissions() throws SQLException {
        return query("SELECT o.object_id_identity || '|' || s.sid || '|' || e.mask FROM acl_entry e"
                + " JOIN acl_object_identity o ON o.id = e.acl_object_identity"
                + " JOIN acl_sid s ON s.id = e.sid ORDER BY o.object_id_identity, s.sid");
    }

    private List<String> authorities() throws SQLException {
        return query("SELECT g.groupName || '|' || a.authority FROM OpenL_Group_Authorities a"
                + " JOIN OpenL_Groups g ON g.id = a.groupID ORDER BY g.groupName, a.authority");
    }

    private void runScripts(String... resources) throws SQLException {
        for (String resource : resources) {
            execute("RUNSCRIPT FROM '" + getClass().getClassLoader().getResource(resource) + "'");
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private boolean tableExists(String name) throws SQLException {
        return !query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'"
                + " AND table_name = '" + name + "'").isEmpty();
    }

    /** Declared type of a column as "type|length", which tells a fixed length from a variable one. */
    private List<String> columnType(String table, String column) throws SQLException {
        return query("SELECT data_type || '|' || character_maximum_length FROM information_schema.columns"
                + " WHERE table_schema = 'PUBLIC' AND table_name = '" + table + "'"
                + " AND column_name = '" + column + "'");
    }

    private boolean indexExists(String name) throws SQLException {
        return !query("SELECT index_name FROM information_schema.indexes WHERE table_schema = 'PUBLIC'"
                + " AND index_name = '" + name + "'").isEmpty();
    }

    /** Tables of the security schema, without the bookkeeping Liquibase keeps for itself. */
    private List<String> tables() throws SQLException {
        return query("SELECT table_name FROM information_schema.tables WHERE table_schema = 'PUBLIC'"
                + " AND table_name NOT LIKE 'DATABASECHANGELOG%' ORDER BY table_name");
    }

    private static String causeMessages(Throwable error) {
        var text = new StringBuilder(String.valueOf(error.getMessage()));
        for (var cause = error.getCause(); cause != null; cause = cause.getCause()) {
            text.append(cause.getMessage());
        }
        return text.toString();
    }

    private List<String> query(String sql) throws SQLException {
        var values = new ArrayList<String>();
        try (Connection connection = dataSource.getConnection();
                var statement = connection.createStatement();
                var rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }
}
