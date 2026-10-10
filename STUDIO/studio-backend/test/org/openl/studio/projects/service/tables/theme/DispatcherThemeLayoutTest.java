package org.openl.studio.projects.service.tables.theme;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.studio.projects.service.tables.TableTestProjects;

/**
 * Verifies that the table the compiler builds to choose between the versions of a rule takes the look of a Rules table.
 *
 * <p>The compiler binds that table outside the module, so the theme reads its parts from the table the node stands for,
 * as it does for any decision table.
 */
class DispatcherThemeLayoutTest {

    private static final String SHEET = "Rules";

    /** The looks the standard theme gives a Rules table. */
    private static final String MUTED = "#808080";
    private static final String TITLE = "#bfbfbf";
    private static final String RETURN_TITLE = "#b4c6e7";
    private static final String RETURN = "#ddebf7";

    /** The rows of the built table: the header, the kinds, the code, the parameters, the titles and the rules. */
    private static final int KINDS = 1;
    private static final int TITLES = 4;
    private static final int FIRST_RULE = 5;

    private final TableThemeService service = new TableThemeService();

    private ProjectModel projectModel;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        projectModel = TableTestProjects.projectModel(dir, SHEET, TableTestProjects::helloInTwoVersions);
    }

    @Test
    void givesTheTableChoosingBetweenVersionsTheLookOfARulesTable() {
        var layout = service.layoutOf(TableTestProjects.dispatcherTable(projectModel));

        assertNotNull(layout);
        assertEquals(MUTED, layout.at(KINDS, 0).style().color().rgb(), "The kinds are code");
        assertEquals(TITLE, layout.at(TITLES, 0).style().background().rgb(), "The title of the condition");
        assertEquals(RETURN_TITLE, layout.at(TITLES, 1).style().background().rgb(), "The title of what it returns");
        assertEquals(RETURN, layout.at(FIRST_RULE, 1).style().background().rgb(), "What a rule returns");
    }
}
