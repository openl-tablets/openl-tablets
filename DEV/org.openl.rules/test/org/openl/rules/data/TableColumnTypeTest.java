package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import org.openl.rules.table.ILogicalTable;
import org.openl.types.java.JavaOpenClass;

/**
 * The type of the values a column of a Data table holds. A reference to the table offers its keys in that type.
 */
class TableColumnTypeTest {

    @Test
    void givesTheTypeOfTheFieldTheColumnFills() {
        var name = column(false);
        when(name.getType()).thenReturn(JavaOpenClass.STRING);

        assertEquals(JavaOpenClass.STRING, tableOf(name).getColumnType(0));
    }

    @Test
    void givesNoTypeForAColumnThatBuildsTheRowItself() {
        assertNull(tableOf(column(true)).getColumnType(0));
    }

    @Test
    void givesNoTypeForAColumnWithNoField() {
        assertNull(tableOf(null).getColumnType(0));
    }

    private static Table tableOf(ColumnDescriptor first) {
        var model = mock(ITableModel.class);
        when(model.getDescriptor(0)).thenReturn(first);
        return new Table(model, mock(ILogicalTable.class));
    }

    private static ColumnDescriptor column(boolean buildsTheRow) {
        var column = mock(ColumnDescriptor.class);
        when(column.isConstructor()).thenReturn(buildsTheRow);
        return column;
    }
}
