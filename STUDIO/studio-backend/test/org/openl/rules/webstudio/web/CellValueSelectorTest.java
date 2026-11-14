package org.openl.rules.webstudio.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ICell;
import org.openl.rules.table.IGridTable;

/**
 * @author Yury Molchan
 */
class CellValueSelectorTest {

    private static final Date MARCH_12 = Date.from(LocalDateTime.of(2014, 3, 12, 0, 0)
            .atZone(ZoneId.systemDefault())
            .toInstant());

    @ParameterizedTest
    @ValueSource(strings = {"3/12/2014", "03/12/2014", "2014-03-12", " 3/12/2014 "})
    void dateCellIsFoundByTheDayWrittenAsText(String text) {
        assertTrue(new CellValueSelector(text).test(table("effectiveDate", MARCH_12)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"3/13/2014", "2015-03-12", "health"})
    void dateCellIsNotFoundByAnotherText(String text) {
        assertFalse(new CellValueSelector(text).test(table("effectiveDate", MARCH_12)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"heal", "HEALTH", "lob"})
    void textCellIsFoundByAPartOfItsText(String text) {
        assertTrue(new CellValueSelector(text).test(table("lob", "health")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"2014-03-12", "auto"})
    void textCellIsNotFoundByAnotherText(String text) {
        assertFalse(new CellValueSelector(text).test(table("lob", "health")));
    }

    private static TableSyntaxNode table(Object... values) {
        var grid = mock(IGridTable.class);
        when(grid.getHeight()).thenReturn(1);
        when(grid.getWidth()).thenReturn(values.length);
        for (var col = 0; col < values.length; col++) {
            var cell = mock(ICell.class);
            when(cell.getObjectValue()).thenReturn(values[col]);
            when(grid.getCell(col, 0)).thenReturn(cell);
        }
        var node = mock(TableSyntaxNode.class);
        when(node.getGridTable()).thenReturn(grid);
        return node;
    }
}
