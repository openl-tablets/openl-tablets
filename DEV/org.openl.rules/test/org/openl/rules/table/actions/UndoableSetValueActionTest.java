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
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.lang.xls.types.meta.MetaInfoWriter;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.IWritableGrid;
import org.openl.types.java.JavaOpenClass;

class UndoableSetValueActionTest {

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
}
