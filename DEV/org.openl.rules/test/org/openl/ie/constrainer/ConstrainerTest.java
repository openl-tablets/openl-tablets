package org.openl.ie.constrainer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class ConstrainerTest {

    private final Constrainer constrainer = new Constrainer();
    private final IntExp x = constrainer.addIntVar(0, 5, "x");
    private final IntExp y = constrainer.addIntVar(0, 5, "y");

    /**
     * Returns the first values of the variables that the search finds for the constraint, or {@code null}.
     */
    private int @Nullable [] solve(Supplier<Goal> constraint, IntExp... vars) {
        var solutions = new ArrayList<int[]>();
        var found = constrainer.solve(constraint, vars, () -> {
            solutions.add(Arrays.stream(vars).mapToInt(IntExp::min).toArray());
            return null;
        });
        assertEquals(found, !solutions.isEmpty());
        return found ? solutions.getFirst() : null;
    }

    /**
     * Returns the goal that imposes both constraints.
     */
    private static Goal both(Goal first, Goal second) {
        return () -> {
            first.execute();
            return second;
        };
    }

    @Test
    void findsTheSmallestValuesInLexicographicOrder() {
        assertArrayEquals(new int[]{3, 2}, solve(() -> x.add(y).eq(5).and(x.gt(y)).equalTo(1), x, y));
        assertArrayEquals(new int[]{0, 5}, solve(() -> x.add(y).eq(5).and(x.lt(y)).equalTo(1), x, y));
        assertArrayEquals(new int[]{1, 0}, solve(() -> x.gt(y).equalTo(1), x, y));
        assertArrayEquals(new int[]{0, 1}, solve(() -> x.lt(y).equalTo(1), x, y));
    }

    @Test
    void findsNothingWhenTheConstraintCannotBeSatisfied() {
        assertNull(solve(() -> x.gt(y).and(y.gt(x)).equalTo(1), x, y));
        assertNull(solve(() -> x.gt(5).equalTo(1), x));
        assertNull(solve(() -> x.add(y).equalTo(11), x, y));
    }

    @Test
    void restoresTheDomainsAfterTheSearch() {
        solve(() -> () -> {
            x.removeValue(2);
            x.setMax(4);
            return x.ge(3).equalTo(1);
        }, x, y);
        solve(() -> x.lt(0).equalTo(1), x);

        assertEquals(0, x.min());
        assertEquals(5, x.max());
        assertTrue(x.contains(2));
        assertEquals(0, y.min());
        assertEquals(5, y.max());
    }

    @Test
    void comparesWithValues() {
        assertArrayEquals(new int[]{3}, solve(() -> x.eq(3).equalTo(1), x));
        assertArrayEquals(new int[]{1}, solve(() -> x.eq(0).equalTo(0), x));
        assertArrayEquals(new int[]{3}, solve(() -> x.ge(3).equalTo(1), x));
        assertArrayEquals(new int[]{4}, solve(() -> x.gt(3).equalTo(1), x));
        assertArrayEquals(new int[]{4}, solve(() -> x.le(3).equalTo(0), x));
        assertArrayEquals(new int[]{3}, solve(() -> x.lt(3).equalTo(0), x));
    }

    @Test
    void skipsTheRemovedValues() {
        assertArrayEquals(new int[]{3}, solve(() -> x.eq(1).or(x.eq(2)).or(x.eq(0)).equalTo(0), x));
        assertArrayEquals(new int[]{4}, solve(() -> both(x.eq(2).or(x.eq(3)).equalTo(0), x.ge(2).equalTo(1)), x));
    }

    @Test
    void stopsASearchThatTakesMoreStepsThanTheLimit() {
        var limited = new Constrainer(100);
        var age = limited.addIntVar(Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, "age");
        // <1, 1-4, 5-10 and a rule for any other age: the rules left after the overlappings found are tried at every
        // age up to the largest whole number
        IntBoolExp[][] rules = {{age.le(0)}, {age.ge(1).and(age.le(4))}, {age.ge(5).and(age.le(10))},
                {limited.constant(true)}};
        var checker = new DTChecker(limited, rules, List.of(age), true);

        assertThrows(SearchLimitExceeded.class, checker::checkOverlappings);

        assertEquals(Integer.MIN_VALUE + 1, age.min());
        assertEquals(Integer.MAX_VALUE - 1, age.max());
    }

    @Test
    void stopsAPropagationThatTakesMoreStepsThanTheLimit() {
        var limited = new Constrainer(1000);
        var a = limited.addIntVar(-1_000_000_000, 1_000_000_000, "a");
        var b = limited.addIntVar(-1_000_000_000, 1_000_000_000, "b");
        // a > b and b > a narrow each other by one value at a time, within the first goal of the search
        Supplier<Goal> contradiction = () -> a.gt(b).and(b.gt(a)).equalTo(1);

        assertThrows(SearchLimitExceeded.class, () -> limited.solve(contradiction, new IntExp[]{a, b}, () -> null));

        assertEquals(-1_000_000_000, a.min());
        assertEquals(1_000_000_000, b.max());
        // the constrainer takes a new search after the stopped one
        assertTrue(limited.solve(() -> a.eq(1).equalTo(1), new IntExp[]{a}, () -> null));
    }

    @Test
    void searchesLargeDomainsThatKeepOnlyTheirBounds() {
        var big = constrainer.addIntVar(-1000, 1000, "big");

        assertArrayEquals(new int[]{501}, solve(() -> big.eq(500).or(big.lt(500)).equalTo(0), big));
        assertArrayEquals(new int[]{-999}, solve(() -> big.eq(-1000).equalTo(0), big));
    }

    @Test
    void addsValues() {
        assertArrayEquals(new int[]{2}, solve(() -> x.add(3).eq(5).equalTo(1), x));
        assertArrayEquals(new int[]{4}, solve(() -> x.add(1).add(-2).eq(3).equalTo(1), x));
        assertArrayEquals(new int[]{1}, solve(() -> both(x.add(2).ge(3).equalTo(1), x.add(2).eq(3).equalTo(1)), x));
        assertArrayEquals(new int[]{2}, solve(() -> both(x.add(2).eq(3).equalTo(0), x.add(2).eq(2).equalTo(0)), x));
        assertArrayEquals(new int[]{5}, solve(() -> x.add(10).equalTo(15), x));
    }

    @Test
    void addsExpressions() {
        assertArrayEquals(new int[]{4, 5}, solve(() -> x.add(y).ge(9).equalTo(1), x, y));
        assertArrayEquals(new int[]{0, 5}, solve(() -> x.add(y).equalTo(5), x, y));
        assertArrayEquals(new int[]{0, 2}, solve(() -> x.add(y).eq(1).or(x.add(y).eq(0)).equalTo(0), x, y));
        assertArrayEquals(new int[]{5, 5}, solve(() -> x.add(y).le(9).equalTo(0), x, y));
    }

    @Test
    void failsWhenTheTermsOfABoundSumExceedItsValue() {
        assertNull(solve(() -> () -> {
            x.add(y).equalTo(5).execute();
            y.setMin(2);
            x.setMin(4);
            return null;
        }, x, y));
    }

    @Test
    void sumsTerms() {
        var z = constrainer.addIntVar(0, 5, "z");
        IntExp[] terms = {x, y, z};

        assertArrayEquals(new int[]{3, 5, 5},
                solve(() -> IntExpAddArray.sum(constrainer, terms).ge(13).equalTo(1), terms));
        assertArrayEquals(new int[]{5, 5, 5}, solve(() -> IntExpAddArray.sum(constrainer, terms).equalTo(15), terms));
        assertArrayEquals(new int[]{0, 1, 3},
                solve(() -> both(IntExpAddArray.sum(constrainer, terms).equalTo(4), y.eq(0).equalTo(0)), terms));
        assertArrayEquals(new int[]{0, 0, 2},
                solve(() -> IntExpAddArray.sum(constrainer, terms).eq(1).or(z.lt(2)).equalTo(0), terms));
        assertNull(solve(() -> IntExpAddArray.sum(constrainer, terms).equalTo(16), terms));
    }

    @Test
    void combinesBooleans() {
        var b = constrainer.addIntBoolVar("b");

        assertArrayEquals(new int[]{1, 0}, solve(() -> b.and(x.eq(0)).equalTo(1), b, x));
        assertArrayEquals(new int[]{0, 0}, solve(() -> b.and(x.gt(2)).equalTo(0), b, x));
        assertArrayEquals(new int[]{1, 0}, solve(() -> b.or(x.gt(5)).equalTo(1), b, x));
        assertArrayEquals(new int[]{0, 3}, solve(() -> both(b.or(x.eq(3)).equalTo(1), b.eq(0).equalTo(1)), b, x));
        assertArrayEquals(new int[]{0, 0}, solve(() -> x.eq(1).or(b).equalTo(0), b, x));
        assertArrayEquals(new int[]{0, 1}, solve(() -> x.eq(1).and(b.eq(0)).equalTo(1), b, x));
    }
}
