package org.openl.rules.table.actions;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.openl.rules.enumeration.CountriesEnum;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.types.java.JavaOpenClass;

class UndoableSetValueActionTest {

    /** An enum whose constant has a body of its own, so the class of the constant is not the enum. */
    enum Speed {
        FAST {
            @Override
            public String toString() {
                return "fast";
            }
        },
        SLOW
    }

    @Test
    void preservesConvertedEnumArrayComponentType() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var cell = mock(ICell.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        var enumType = JavaOpenClass.getOpenClass(CountriesEnum.class);
        when(table.getGrid()).thenReturn(grid);
        when(grid.getCell(0, 0)).thenReturn(cell);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(new CellMetaInfo(enumType, true));

        new UndoableSetValueAction(0, 0, new Object[]{"US", "UA"}, metaInfoWriter).doAction(table);

        var valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(grid).setCellValue(eq(0), eq(0), valueCaptor.capture());
        var values = assertInstanceOf(CountriesEnum[].class, valueCaptor.getValue());
        assertArrayEquals(new CountriesEnum[]{CountriesEnum.US, CountriesEnum.UA}, values);
    }

    @Test
    void convertsMultiValueStringsToTheCellElementType() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var cell = mock(ICell.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        var dateType = JavaOpenClass.getOpenClass(Date.class);
        var dateArrayMetaInfo = new CellMetaInfo(dateType, true);
        when(table.getGrid()).thenReturn(grid);
        when(grid.getCell(0, 0)).thenReturn(cell);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(dateArrayMetaInfo);

        new UndoableSetValueAction(0,
                0,
                new Object[]{"2024-01-02T00:00:00.000", null},
                metaInfoWriter).doAction(table);

        var valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(grid).setCellValue(eq(0), eq(0), valueCaptor.capture());
        var values = assertInstanceOf(Object[].class, valueCaptor.getValue());
        var date = assertInstanceOf(Date.class, values[0]);
        assertEquals(LocalDate.of(2024, 1, 2), date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        assertNull(values[1]);

        var metaInfoCaptor = ArgumentCaptor.forClass(CellMetaInfo.class);
        verify(metaInfoWriter).setMetaInfo(eq(0), eq(0), metaInfoCaptor.capture());
        assertEquals(dateType, metaInfoCaptor.getValue().getDataType());
        assertTrue(metaInfoCaptor.getValue().isMultiValue());
    }

    @Test
    void recomputesMetadataWhenConvertedArrayContainsMismatchedElement() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var cell = mock(ICell.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        var dateType = JavaOpenClass.getOpenClass(Date.class);
        when(table.getGrid()).thenReturn(grid);
        when(grid.getCell(0, 0)).thenReturn(cell);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(new CellMetaInfo(dateType, true));

        new UndoableSetValueAction(0,
                0,
                new Object[]{"2024-01-02T00:00:00.000", "not-a-date"},
                metaInfoWriter).doAction(table);

        var metaInfoCaptor = ArgumentCaptor.forClass(CellMetaInfo.class);
        verify(metaInfoWriter).setMetaInfo(eq(0), eq(0), metaInfoCaptor.capture());
        assertEquals(JavaOpenClass.getOpenClass(Object.class), metaInfoCaptor.getValue().getDataType());
        assertTrue(metaInfoCaptor.getValue().isMultiValue());
    }

    @Test
    void writesARangeAsItWasTyped() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        var rangeType = JavaOpenClass.getOpenClass(IntRange.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(new CellMetaInfo(rangeType, false));

        new UndoableSetValueAction(0, 0, "2-5", metaInfoWriter).doAction(table);

        // Parsed to check it, but written in the notation its author typed rather than in the range's own
        verify(grid).setCellValue(0, 0, "2-5");
        var metaInfoCaptor = ArgumentCaptor.forClass(CellMetaInfo.class);
        verify(metaInfoWriter).setMetaInfo(eq(0), eq(0), metaInfoCaptor.capture());
        assertEquals(rangeType, metaInfoCaptor.getValue().getDataType());
    }

    @Test
    void writesANumberAsTheNumberItReadsAs() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(
                new CellMetaInfo(JavaOpenClass.getOpenClass(Integer.class), false));

        new UndoableSetValueAction(0, 0, "42", metaInfoWriter).doAction(table);

        // A number is stored as an Excel number, not as the text typed
        verify(grid).setCellValue(0, 0, 42);
    }

    @Test
    void writesAnEnumConstantWithABodyAsTheConstant() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(
                new CellMetaInfo(JavaOpenClass.getOpenClass(Speed.class), false));

        new UndoableSetValueAction(0, 0, "FAST", metaInfoWriter).doAction(table);

        verify(grid).setCellValue(0, 0, Speed.FAST);
    }

    @Test
    void writesABooleanAsItWasTyped() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(
                new CellMetaInfo(JavaOpenClass.getOpenClass(Boolean.class), false));

        new UndoableSetValueAction(0, 0, "Y", metaInfoWriter).doAction(table);

        // OpenL reads Y as true, and the author's Y stays in the cell rather than an Excel TRUE
        verify(grid).setCellValue(0, 0, "Y");
        // The cell is still a boolean one until the module is compiled again, not a text
        var metaInfoCaptor = ArgumentCaptor.forClass(CellMetaInfo.class);
        verify(metaInfoWriter).setMetaInfo(eq(0), eq(0), metaInfoCaptor.capture());
        assertEquals(JavaOpenClass.getOpenClass(Boolean.class), metaInfoCaptor.getValue().getDataType());
    }

    @Test
    void keepsTheElementTypeOfBooleansTypedIntoAList() {
        var table = mock(IGridTable.class);
        var grid = mock(IWritableGrid.class);
        var metaInfoWriter = mock(MetaInfoWriter.class);
        var booleanType = JavaOpenClass.getOpenClass(Boolean.class);
        when(table.getGrid()).thenReturn(grid);
        when(metaInfoWriter.getMetaInfo(0, 0)).thenReturn(new CellMetaInfo(booleanType, true));

        new UndoableSetValueAction(0, 0, new Object[]{"Y", "N"}, metaInfoWriter).doAction(table);

        var valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(grid).setCellValue(eq(0), eq(0), valueCaptor.capture());
        assertArrayEquals(new Object[]{"Y", "N"}, (Object[]) valueCaptor.getValue());
        // Still a list of booleans until the module is compiled again
        var metaInfoCaptor = ArgumentCaptor.forClass(CellMetaInfo.class);
        verify(metaInfoWriter).setMetaInfo(eq(0), eq(0), metaInfoCaptor.capture());
        assertEquals(booleanType, metaInfoCaptor.getValue().getDataType());
        assertTrue(metaInfoCaptor.getValue().isMultiValue());
    }
}
