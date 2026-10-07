package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.SearchScope;
import org.openl.studio.projects.model.tables.TableRunState;

/**
 * Verifies that a table its author named as the compiler names the table choosing between the versions of a rule is
 * the author's own: written in the workbook, it is listed, run and given properties as any other table.
 *
 * <p>Only the table the compiler built, which sits in a workbook of the compiler's own, is read and nothing else.
 */
class AuthorTableNamedLikeGeneratedTest {

    private static final String NAME = "validateGapOverlapCheck";

    private ProjectModel model;

    @TempDir
    Path dir;

    @BeforeEach
    void writeProject() throws IOException {
        model = TableTestProjects.projectModel(dir, "Rules", sheet -> {
            TableTestProjects.helloInTwoVersions(sheet);
            TableTestProjects.row(sheet, 13, 1, "SmartRules String " + NAME + "(String name)");
            TableTestProjects.row(sheet, 14, 1, "Name", "Greeting");
            TableTestProjects.row(sheet, 15, 1, "x", "Checked");
        });
    }

    @Test
    void isListedWithTheTablesOfTheModule() {
        var listed = model.search(node -> true, SearchScope.CURRENT_MODULE);

        assertTrue(listed.stream().anyMatch(table -> NAME.equals(table.getName())), "the author's table is listed");
        // The table the compiler built is still kept out of the list.
        var generated = TableTestProjects.dispatcherTable(model);
        assertFalse(listed.stream().anyMatch(table -> generated.getUri().equals(table.getUri())));
    }

    @Test
    void isRunAndGivenPropertiesAsAnyOtherTable() {
        var table = TableTestProjects.table(model, NAME);

        assertFalse(model.isGeneratedTable(table));
        assertTrue(model.holdsTable(table.getUri()));
        assertEquals(TableRunState.CAN_RUN_MODULE, new TableRunStateService().of(model, table));
        assertTrue(TablePropertyRules.canEditProperties(table));
    }

    @Test
    void theTableTheCompilerBuiltIsOnlyRead() {
        var generated = TableTestProjects.dispatcherTable(model);

        assertTrue(model.isGeneratedTable(generated));
        assertEquals(TableRunState.CANNOT_RUN, new TableRunStateService().of(model, generated));
        assertFalse(TablePropertyRules.canEditProperties(generated));
    }
}
