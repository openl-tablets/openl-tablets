package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.BLOCK;
import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.OVERRIDE;
import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.PARTIAL;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.ie.constrainer.Overlapping.OverlappingStatus;

class DTCheckerTest {

    private final Constrainer constrainer = new Constrainer();
    private final IntExp x = constrainer.addIntVar(0, 4, "x");
    private final IntExp y = constrainer.addIntVar(0, 1, "y");

    private DTChecker checker(boolean overrideAscending, IntBoolExp[]... conditions) {
        return new DTChecker(constrainer, conditions, List.of(x, y), overrideAscending);
    }

    private static void assertOverlapping(Overlapping overlapping,
                                          OverlappingStatus status,
                                          int[] rules,
                                          int... values) {
        assertEquals(status, overlapping.getStatus());
        assertArrayEquals(rules, overlapping.getOverlapped());
        assertArrayEquals(new String[]{"x", "y"}, overlapping.getSolutionNames());
        assertArrayEquals(values, overlapping.getSolutionValues());
    }

    @Test
    void reportsTheSmallestUncoveredInput() {
        var uncovered = checker(true,
                new IntBoolExp[]{x.le(1), y.eq(0)},
                new IntBoolExp[]{x.ge(3), constrainer.constant(true)},
                new IntBoolExp[]{x.eq(2), y.eq(1)}).checkCompleteness();

        assertEquals(1, uncovered.size());
        assertArrayEquals(new String[]{"x", "y"}, uncovered.getFirst().getSolutionNames());
        assertArrayEquals(new int[]{0, 1}, uncovered.getFirst().getSolutionValues());
    }

    @Test
    void reportsNoUncoveredInputWhenTheRulesCoverAll() {
        assertTrue(checker(true, new IntBoolExp[]{x.le(2)}, new IntBoolExp[]{x.ge(2)}).checkCompleteness().isEmpty());
        assertTrue(checker(true, new IntBoolExp[]{constrainer.constant(true)}).checkCompleteness().isEmpty());
    }

    @Test
    void reportsEveryInputAsUncoveredWithoutRules() {
        var checker = checker(true);

        var uncovered = checker.checkCompleteness();
        assertEquals(1, uncovered.size());
        assertArrayEquals(new int[]{0, 0}, uncovered.getFirst().getSolutionValues());
        assertTrue(checker.checkOverlappings().isEmpty());
    }

    @Test
    void reportsNoOverlappingForDisjointRules() {
        assertTrue(checker(true, new IntBoolExp[]{x.le(1)}, new IntBoolExp[]{x.ge(2)}).checkOverlappings().isEmpty());
    }

    @Test
    void reportsThePartialOverlapping() {
        var overlappings = checker(true, new IntBoolExp[]{x.le(2)}, new IntBoolExp[]{x.ge(2)}).checkOverlappings();

        assertEquals(1, overlappings.size());
        assertOverlapping(overlappings.getFirst(), PARTIAL, new int[]{0, 1}, 2, 0);
    }

    @Test
    void reportsTheRuleThatBlocksAnotherOne() {
        var overlappings = checker(true, new IntBoolExp[]{x.ge(0)}, new IntBoolExp[]{x.eq(3), y.eq(1)})
                .checkOverlappings();

        assertEquals(1, overlappings.size());
        assertOverlapping(overlappings.getFirst(), BLOCK, new int[]{0, 1}, 3, 1);
    }

    @Test
    void appliesTheLowerRuleFirstWhenTheOverrideIsDescending() {
        var overlappings = checker(false, new IntBoolExp[]{x.ge(0)}, new IntBoolExp[]{x.eq(3)}).checkOverlappings();

        assertEquals(1, overlappings.size());
        assertOverlapping(overlappings.getFirst(), OVERRIDE, new int[]{1, 0}, 3, 0);
    }

    @Test
    void reportsEveryPairOfTheRulesThatCoverTheSameInputOnce() {
        var overlappings = checker(true,
                new IntBoolExp[]{x.ge(0)},
                new IntBoolExp[]{x.ge(0)},
                new IntBoolExp[]{x.ge(0)}).checkOverlappings();

        assertEquals(3, overlappings.size());
        assertOverlapping(overlappings.get(0), BLOCK, new int[]{0, 1}, 0, 0);
        assertOverlapping(overlappings.get(1), BLOCK, new int[]{1, 2}, 0, 0);
        assertOverlapping(overlappings.get(2), BLOCK, new int[]{0, 2}, 0, 0);
    }

    @Test
    void stopsSearchingForMoreOverlappingsAfterFifty() {
        var c = new Constrainer();
        var v = c.addIntVar(0, 3, "v");
        int[][] ranges = {{1, 2}, {0, 2}, {1, 1}, {1, 3}, {0, 3}, {2, 2}, {0, 1}, {0, 2}, {0, 0}, {3, 3}, {1, 3},
                {3, 3}, {2, 3}, {1, 2}, {1, 3}, {1, 1}, {1, 3}, {1, 2}, {1, 2}};
        var conditions = Arrays.stream(ranges)
                .map(range -> new IntBoolExp[]{v.ge(range[0]).and(v.le(range[1]))})
                .toArray(IntBoolExp[][]::new);

        // Without the limit, the search finds 71 overlappings.
        assertEquals(61, new DTChecker(c, conditions, List.of(v), true).checkOverlappings().size());
    }

    @Test
    void searchesAgainWithoutTheRuleThatOverlaps() {
        var overlappings = checker(true,
                new IntBoolExp[]{x.eq(0)},
                new IntBoolExp[]{x.le(1)},
                new IntBoolExp[]{x.ge(1)}).checkOverlappings();

        assertEquals(2, overlappings.size());
        assertOverlapping(overlappings.get(0), OVERRIDE, new int[]{0, 1}, 0, 0);
        assertOverlapping(overlappings.get(1), PARTIAL, new int[]{1, 2}, 1, 0);
    }
}
