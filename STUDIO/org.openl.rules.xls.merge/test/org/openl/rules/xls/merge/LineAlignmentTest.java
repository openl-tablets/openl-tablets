package org.openl.rules.xls.merge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

class LineAlignmentTest {

    private static final LineAlignment.Similarity NOT_ALIKE = (baseLine, sideLine) -> -1;

    @Test
    void longTableChangedAtBothEnds_isAlignedByItsUniqueLines() {
        // The changed first and last lines leave a middle too long to compare every line with every other one.
        var base = LongStream.range(0, 3000).toArray();
        var side = LongStream.range(0, 3001).map(i -> i <= 1500 ? i : i - 1).toArray();
        side[0] = -1;
        side[1500] = -2;
        side[3000] = -3;
        var alignment = assertAligned(base, side);
        assertEquals(List.of(new LineAlignment.Hunk(1500, 1500, 1500, 1501)), alignment.hunks());
        assertEquals(0, alignment.sideOf()[0]);
        assertEquals(1501, alignment.sideOf()[1500]);
        assertEquals(3000, alignment.sideOf()[2999]);
    }

    @Test
    void longTableOfRepeatedLines_cannotBeAligned() {
        // Every line of the long middle repeats, so nothing tells where its lines went.
        var base = new long[3000];
        var side = new long[3001];
        base[0] = 1;
        base[2999] = 1;
        side[0] = 2;
        side[3000] = 2;
        assertNull(LineAlignment.align(base, side, NOT_ALIKE));
    }

    private static LineAlignment assertAligned(long[] base, long[] side) {
        var alignment = LineAlignment.align(base, side, NOT_ALIKE);
        assertNotNull(alignment);
        return alignment;
    }
}
