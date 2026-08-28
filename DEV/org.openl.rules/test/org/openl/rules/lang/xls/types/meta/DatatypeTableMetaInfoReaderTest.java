package org.openl.rules.lang.xls.types.meta;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.rules.datatype.binding.DatatypeHelper;
import org.openl.rules.datatype.binding.DatatypeTableBoundNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.ILogicalTable;

class DatatypeTableMetaInfoReaderTest {

    @Test
    void returnsNoMetaInfoForRowsOutsideBoundLogicalTable() {
        var boundNode = mock(DatatypeTableBoundNode.class);
        var logicalTable = mock(ILogicalTable.class);
        var firstCell = mock(ICell.class);
        when(boundNode.getTable()).thenReturn(logicalTable);
        when(boundNode.getColumnTitlesOrder()).thenReturn(Map.of(DatatypeHelper.DEFAULT_COLUMN_TITLE, 2));
        when(logicalTable.getCell(0, 0)).thenReturn(firstCell);
        when(logicalTable.isNormalOrientation()).thenReturn(true);
        when(logicalTable.getHeight()).thenReturn(5);
        when(logicalTable.getRow(5)).thenThrow(ArrayIndexOutOfBoundsException.class);

        assertNull(new DatatypeTableMetaInfoReader(boundNode).getBodyMetaInfo(5, 2));
        verify(logicalTable, never()).getRow(anyInt());
    }
}
