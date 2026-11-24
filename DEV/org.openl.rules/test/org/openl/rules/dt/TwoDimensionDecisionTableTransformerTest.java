package org.openl.rules.dt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.openl.rules.datatype.binding.MockGridTable;

/**
 * Covers a lookup whose one key is merged over all its value columns: the return is three columns wide, while one
 * column of the table stands after the return header.
 */
class TwoDimensionDecisionTableTransformerTest {

    /**
     * The table as the compiler reads it: three header rows, one row of keys and two rows of values, one vertical
     * condition column and three value columns.
     */
    private static TwoDimensionDecisionTableTransformer transformer() {
        return new TwoDimensionDecisionTableTransformer(cells(6, 4), cells(2, 3), 1, new int[]{0}, new int[]{0, 3});
    }

    /** A grid of filled cells: only the size matters to the transformer. */
    private static MockGridTable cells(int height, int width) {
        var values = new String[height][width];
        for (var row : values) {
            Arrays.fill(row, "x");
        }
        return new MockGridTable(values);
    }

    @Test
    void theHeadersOfAWideReturnStandInTheTable() {
        var transformer = transformer();

        // The vertical condition, the horizontal one and the return, three columns wide
        assertEquals(5, transformer.getWidth());
        assertEquals(3, transformer.getCellWidth(2, 0));
        for (var row = 0; row < 4; row++) {
            assertFalse(transformer.isEmpty(2, row));
            assertEquals(2, transformer.getColumn(2, row));
            for (var column = 3; column < 5; column++) {
                // The columns of the return after its header are empty and stand on the return column
                assertTrue(transformer.isEmpty(column, row));
                assertEquals(2, transformer.getColumn(column, row));
            }
        }
    }

    @Test
    void theHeadersOfAWideReturnStandOnTheReturnHeaderWrittenBeforeAHorizontalCondition() {
        // C1 | HC1 | RET1 | HC2: the return header stands on column 2, the second horizontal condition on column 3
        var transformer = new LookupHeadersTransformer(cells(7, 5),
                cells(2, 4),
                1,
                new int[]{0, 1},
                new int[]{0, 4},
                1,
                new int[]{1, 3, 2});

        // The return is the fourth column of the logical table, four columns wide
        for (var column = 3; column < 7; column++) {
            assertEquals(2, transformer.getColumn(column, 0));
        }
        assertEquals(1, transformer.getColumn(1, 0));
        assertEquals(3, transformer.getColumn(2, 0));
    }

    @Test
    void theRuleReturnsTheValuesUnderItsKey() {
        var transformer = transformer();

        // One group of three columns over two rows of values: two rules
        assertEquals(6, transformer.getHeight());
        for (var column = 2; column < 5; column++) {
            assertFalse(transformer.isEmpty(column, 4));
            assertEquals(column - 1, transformer.getColumn(column, 4));
            assertEquals(4, transformer.getRow(column, 4));
            assertEquals(5, transformer.getRow(column, 5));
        }
        // The key of the horizontal condition over the group
        assertEquals(1, transformer.getColumn(1, 4));
        assertEquals(3, transformer.getRow(1, 4));
    }
}
