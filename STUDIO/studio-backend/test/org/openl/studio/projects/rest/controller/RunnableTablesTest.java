package org.openl.studio.projects.rest.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.ProjectModel;
import org.openl.studio.common.exception.BadRequestException;
import org.openl.studio.common.exception.NotFoundException;

/**
 * Verifies the table a run, a trace, a benchmark or a test run starts from: a table the compiler built to choose
 * between the versions of a rule is refused, saying why, as the Editor never offered to run it.
 */
class RunnableTablesTest {

    private final ProjectModel model = mock(ProjectModel.class);
    private final IOpenLTable table = mock(IOpenLTable.class);

    @Test
    void runsATableTheWorkbookHolds() {
        when(model.getTableById("t-1")).thenReturn(table);

        assertSame(table, RunnableTables.require(model, "t-1"));
    }

    @Test
    void refusesATableThatIsNotThere() {
        var refused = assertThrows(NotFoundException.class, () -> RunnableTables.require(model, "t-gone"));

        assertEquals("openl.error.404.table.message", refused.getErrorCode());
    }

    @Test
    void refusesTheTableChoosingBetweenTheVersionsOfARule() {
        when(model.getTableById("t-dispatch")).thenReturn(table);
        when(model.isGeneratedTable(table)).thenReturn(true);

        var refused = assertThrows(BadRequestException.class, () -> RunnableTables.require(model, "t-dispatch"));

        // The table is there: the action is what is refused, and the reader is told why.
        assertEquals("openl.error.400.table.generated.message", refused.getErrorCode());
    }
}
