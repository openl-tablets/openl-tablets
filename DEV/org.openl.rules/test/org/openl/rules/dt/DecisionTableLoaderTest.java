package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.openl.rules.dt.element.FunctionalRow;
import org.openl.rules.lang.xls.TestWorkbooks;

/** Covers which reading of a decision table the table holds when it fails to compile both ways round. */
class DecisionTableLoaderTest {

    @TempDir
    Path dir;

    @Test
    void holdsTheRowsOfTheReadingItsErrorsComeFrom() throws IOException {
        // The value 3.2 is no Integer, so the lookup fails as it is written and transposed. The first reading is
        // kept: the horizontal condition takes the risks across the row of titles, not the ratings down the table.
        var table = compile(
                new String[]{"SmartLookup Integer Rating(Double financialRating, String risk)"},
                new String[]{"financialRating", "LOW", "HIGH"},
                new String[]{"1", "123", ""},
                new String[]{"345", "4", "3.2"});

        assertNotNull(table.getDtInfo());
        assertFalse(table.getDtInfo().isTransposed(), "The kept reading reads the lookup as it is written");
        var horizontal = Arrays.stream(table.getConditionRows())
                .map(FunctionalRow.class::cast)
                .filter(row -> row.getName().startsWith(DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()))
                .findFirst()
                .orElseThrow();
        var value = horizontal.getValueCell(0).getSource().getCell(0, 0);
        assertEquals(1, value.getAbsoluteRow(), "A risk stands in the row of titles");
        assertEquals("LOW", value.getStringValue());
    }

    @Test
    void startsEachReadingFromATableHoldingNothing() {
        // What an earlier reading loaded is gone, so a reading that fails before it binds its rows leaves none.
        var table = new DecisionTable();
        table.setConditionRows(new IBaseCondition[]{mock(IBaseCondition.class)});
        table.setDtInfo(new DTInfo(1, 1, true));

        table.restore(DecisionTable.Loaded.NONE);

        assertNull(table.getConditionRows(), "No rows of an earlier reading are left");
        assertNull(table.getDtInfo(), "No layout of an earlier reading is left");
    }

    /** The one decision table of a sheet holding the given rows, as the compiler bound it. */
    private DecisionTable compile(String[]... rows) throws IOException {
        var node = TestWorkbooks.compileFirstTable(dir, sheet -> {
            for (var index = 0; index < rows.length; index++) {
                var row = sheet.createRow(index);
                for (var column = 0; column < rows[index].length; column++) {
                    row.createCell(column).setCellValue(rows[index][column]);
                }
            }
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, rows[1].length - 1));
        });
        return (DecisionTable) node.getMember();
    }
}
