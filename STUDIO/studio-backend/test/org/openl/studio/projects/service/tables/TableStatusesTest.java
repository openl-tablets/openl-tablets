package org.openl.studio.projects.service.tables;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

class TableStatusesTest {

    /** A project whose rules are exercised by test tables, and whose spreadsheet does not compile. */
    private static final Path RULES = Path.of("test/rules/EPBDS-16463");

    @Test
    void saysWhichTablesAreExercisedByATest() {
        var model = TableTestProjects.projectModel(RULES);
        var statuses = TableStatuses.of(model);

        var tested = false;
        var untested = false;
        for (TableSyntaxNode node : model.getTableSyntaxNodes()) {
            if (statuses.isTested(node.getUri())) {
                tested = true;
            } else {
                untested = true;
            }
        }

        // A module of rules and the tests covering them has both, and a test table itself is not tested.
        assertTrue(tested, "a table some test exercises is marked");
        assertTrue(untested, "a table nothing exercises is not");
    }

    @Test
    void countsTheErrorsOfEachTableAndNothingOfTheTablesThatCompiled() {
        var model = TableTestProjects.projectModel(RULES);
        var statuses = TableStatuses.of(model);

        var counted = 0;
        for (TableSyntaxNode node : model.getTableSyntaxNodes()) {
            counted += statuses.errorsOf(node.getUri());
        }

        // Every error the compilation raised about a table of this module is counted against that table.
        assertEquals(model.getModuleMessages()
                .stream()
                .filter(message -> message.getSeverity() == org.openl.message.Severity.ERROR)
                .count(), counted, "every error belongs to the table it was raised in");
        assertFalse(statuses.isTested("nothing/is/written/here"), "a place no table covers is not tested");
        assertEquals(0, statuses.errorsOf("nothing/is/written/here"));
    }
}
