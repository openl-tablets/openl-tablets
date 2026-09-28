package org.openl.rules.table.openl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.openl.binding.IBindingContext;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;

/**
 * In execution mode the module reads the code and the address of its cell at once and releases the table, so both
 * stay available without it.
 */
class GridCellSourceCodeModuleTest {

    private final IGridTable table = mock(IGridTable.class);
    private final IBindingContext executionContext = mock(IBindingContext.class);

    @BeforeEach
    void setUp() {
        var cell = mock(ICell.class);
        when(cell.getStringValue()).thenReturn("x + 1");
        when(table.getCell(0, 0)).thenReturn(cell);
        when(executionContext.isExecutionMode()).thenReturn(true);
    }

    @Test
    void keepsTheCodeAndTheUriOfAReleasedTable() {
        when(table.getUri(0, 0)).thenReturn("Rules.xlsx?sheet=Main&cell=B3");

        var module = new GridCellSourceCodeModule(table, executionContext);

        assertEquals("x + 1", module.getCode());
        assertEquals("Rules.xlsx?sheet=Main&cell=B3", module.getUri());
    }

    // A cell without an address keeps having none once its table is released, rather than failing an error report.
    @Test
    void hasNoUriWhenTheReleasedTableGaveNone() {
        var module = new GridCellSourceCodeModule(table, executionContext);

        assertNull(module.getUri());
    }

    @Test
    void readsTheUriOnDemandOutsideExecutionMode() {
        when(table.getUri(0, 0)).thenReturn("Rules.xlsx?sheet=Main&cell=B3");

        var module = new GridCellSourceCodeModule(table);

        assertEquals("Rules.xlsx?sheet=Main&cell=B3", module.getUri());
    }
}
