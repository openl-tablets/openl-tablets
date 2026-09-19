package org.openl.rules.webstudio.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests of reading and removing the project tags kept in the database before 6.0.0.
 *
 * @author Yury Molchan
 */
class LegacyProjectTagsTest {

    private LegacyTagsDatabase database;
    private LegacyProjectTags legacyTags;

    @BeforeEach
    void init() throws Exception {
        useDatabase(new LegacyTagsDatabase());
    }

    private void useDatabase(LegacyTagsDatabase newDatabase) throws SQLException {
        if (database != null) {
            database.close();
        }
        database = newDatabase;
        legacyTags = new LegacyProjectTags(database.dataSource());
    }

    @AfterEach
    void shutDown() throws SQLException {
        database.close();
    }

    @Test
    void theLegacyTablesOfAnUpgradedInstallationLeaveWorkToDo() throws SQLException {
        assertTrue(legacyTags.exists());
    }

    @Test
    void droppedTablesLeaveNothingToDo() throws SQLException {
        database.addTaggedProject("design", "DESIGN/rules/Example");

        legacyTags.drop();

        assertFalse(legacyTags.exists());
        assertEquals(List.of(), legacyTags.read());
        // The tags themselves are still in use by OpenL Studio and stay.
        assertEquals(1, database.count("SELECT count(*) FROM OpenL_Tags"));
        assertEquals(1, database.count("SELECT count(*) FROM OpenL_Tag_Types"));
    }

    @Test
    void aDropThatStoppedHalfwayIsFinished() throws SQLException {
        database.execute("DROP TABLE OpenL_Project_Tags");

        assertTrue(legacyTags.exists());
        assertEquals(List.of(), legacyTags.read());

        legacyTags.drop();

        assertFalse(legacyTags.exists());
        assertFalse(database.tableExists("OpenL_Projects"));
    }

    @Test
    void emptyTablesLeaveNothingToMigrate() throws SQLException {
        assertEquals(List.of(), legacyTags.read());
    }

    @Test
    void everyTagOfAProjectIsCollected() throws SQLException {
        database.addTagType(1, "Environment");
        database.addTagType(2, "Owner");
        database.addTag(10, 1, "production");
        database.addTag(20, 2, "team-a");
        database.addProject(100, "design", "DESIGN/rules/Example");
        database.tagProject(100, 10);
        database.tagProject(100, 20);

        var projects = legacyTags.read();

        assertEquals(1, projects.size());
        var project = projects.getFirst();
        assertEquals("design", project.repositoryId());
        assertEquals("DESIGN/rules/Example", project.projectPath());
        assertEquals(Map.of("Environment", "production", "Owner", "team-a"), project.tags());
    }

    @Test
    void aProjectWithoutTagsIsNotCollected() throws SQLException {
        database.addTagType(1, "Environment");
        database.addTag(10, 1, "production");
        database.addProject(100, "design", "DESIGN/rules/Tagged");
        database.addProject(200, "design", "DESIGN/rules/Untagged");
        database.tagProject(100, 10);

        var projects = legacyTags.read();

        assertEquals(1, projects.size());
        assertEquals("DESIGN/rules/Tagged", projects.getFirst().projectPath());
    }

    @Test
    void lowerCasedTablesAreFound() throws Exception {
        // A database that keeps unquoted names in lower case, such as PostgreSQL or MySQL.
        useDatabase(new LegacyTagsDatabase(";DATABASE_TO_LOWER=TRUE"));
        database.addTaggedProject("design", "DESIGN/rules/Example");

        assertTrue(legacyTags.exists());
        var projects = legacyTags.read();

        assertEquals(1, projects.size());
        assertTrue(projects.getFirst().tags().containsKey("Environment"));
    }
}
