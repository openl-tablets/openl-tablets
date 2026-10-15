package org.openl.studio.projects.service.tables.format;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.model.tables.RawTableRole;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Verifies that the table the compiler builds to choose between the versions of a rule takes the roles of a Rules
 * table.
 *
 * <p>The compiler binds that table outside the module, so the layout reads its parts from the table the node stands
 * for, as it does for any decision table.
 */
class DispatcherLayoutTest {

    private static final String SHEET = "Rules";

    /** The rows of the built table: the header, the kinds, the code, the parameters, the titles and the rules. */
    private static final int KINDS = 1;
    private static final int TITLES = 4;
    private static final int FIRST_RULE = 5;

    private ProjectModel projectModel;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        projectModel = TableTestProjects.projectModel(dir, SHEET, TableTestProjects::helloInTwoVersions);
    }

    @Test
    void givesTheTableChoosingBetweenVersionsTheRolesOfARulesTable() {
        var roles = Roles.of(TableTestProjects.dispatcherTable(projectModel));

        assertEquals(RawTableRole.CODE, roles.roleAt(KINDS, 0), "The kinds are code");
        assertEquals(RawTableRole.INPUT_TITLE, roles.roleAt(TITLES, 0), "The title of the condition");
        assertEquals(RawTableRole.OUTPUT_TITLE, roles.roleAt(TITLES, 1), "The title of what it returns");
        assertEquals(RawTableRole.OUTPUT, roles.roleAt(FIRST_RULE, 1), "What a rule returns");
    }
}
