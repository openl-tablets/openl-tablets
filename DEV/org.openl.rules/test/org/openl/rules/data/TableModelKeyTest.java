package org.openl.rules.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

/**
 * The column a reference reads a row of a Data table by when it names no column: the key of the table.
 */
class TableModelKeyTest {

    @Test
    void keysATableByItsFirstField() {
        assertEquals(0, keyOf(field(0, false), field(1, false)));
    }

    @Test
    void keysATableByAPrimaryKeyWrittenFirst() {
        assertEquals(2, keyOf(field(2, true), field(3, false)));
    }

    @Test
    void keysATableByItsFirstFieldWhenAPrimaryKeyIsWrittenAfterIt() {
        assertEquals(0, keyOf(field(0, false), field(1, true)));
    }

    @Test
    void keysATableWhoseFirstColumnIsTheKeyOfAnotherLevelByItsFirstField() {
        var model = model(field(1, false));
        var otherLevel = field(0, true);
        when(model.getDescriptor(0)).thenReturn(otherLevel);

        assertEquals(1, model.getKeyColumnIndex());
    }

    @Test
    void keysATableOfNoFieldsByItsFirstColumn() {
        assertEquals(0, keyOf());
    }

    private static int keyOf(ColumnDescriptor... fields) {
        return model(fields).getKeyColumnIndex();
    }

    private static ITableModel model(ColumnDescriptor... fields) {
        var model = mock(ITableModel.class, CALLS_REAL_METHODS);
        when(model.getDescriptors()).thenReturn(fields);
        for (var field : fields) {
            when(model.getDescriptor(field.getColumnIdx())).thenReturn(field);
        }
        return model;
    }

    private static ColumnDescriptor field(int column, boolean primaryKey) {
        var field = mock(ColumnDescriptor.class);
        when(field.getColumnIdx()).thenReturn(column);
        when(field.isPrimaryKey()).thenReturn(primaryKey);
        return field;
    }
}
