package org.openl.ie.constrainer;

import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.BLOCK;
import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.OVERRIDE;
import static org.openl.ie.constrainer.Overlapping.OverlappingStatus.PARTIAL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.ie.constrainer.Overlapping.OverlappingStatus;

/**
 * Checks the rules of a decision table for an input that no rule covers, and for the inputs that two rules cover.
 * <p>
 * A rule is the conjunction of its conditions, and an input is a value of every decision table parameter. A check
 * searches the inputs in ascending order of the parameter values, so it reports the smallest inputs.
 */
public final class DTChecker {

    // The search is slow when many rules overlap, so it stops early and the user does not see all the
    // overlappings. EPBDS-5694
    private static final int MAX_OVERLAPPINGS = 50;

    private final Constrainer constrainer;
    private final IntBoolExp[] rules;
    private final IntExp[] vars;
    private final String[] names;
    private final boolean overrideAscending;

    /**
     * Creates the checker of the rules.
     *
     * @param conditions        the conditions of every rule
     * @param vars              the decision table parameters
     * @param overrideAscending whether a rule is applied before the rules below it. It is the case when the decision
     *                          table returns the result of the first rule that matches. Otherwise, a rule is applied
     *                          before the rules above it.
     */
    public DTChecker(Constrainer constrainer, IntBoolExp[][] conditions, List<IntExp> vars, boolean overrideAscending) {
        this.constrainer = constrainer;
        this.rules = Arrays.stream(conditions).map(this::conjunction).toArray(IntBoolExp[]::new);
        this.vars = vars.toArray(IntExp[]::new);
        this.names = vars.stream().map(IntExp::name).toArray(String[]::new);
        this.overrideAscending = overrideAscending;
    }

    private IntBoolExp conjunction(IntBoolExp[] conditions) {
        var rule = constrainer.constant(true);
        for (var condition : conditions) {
            rule = rule.and(condition);
        }
        return rule;
    }

    /**
     * Returns the input that no rule covers, if any.
     */
    public List<Uncovered> checkCompleteness() {
        var uncovered = new ArrayList<Uncovered>(1);
        constrainer.solve(() -> IntExpAddArray.sum(constrainer, rules).equalTo(0), vars, () -> {
            uncovered.add(new Uncovered(names, values()));
            return null;
        });
        return uncovered;
    }

    /**
     * Returns the pairs of rules that cover the same input, with an input for every pair.
     */
    public List<Overlapping> checkOverlappings() {
        var search = new OverlappingSearch();
        search.check();
        return search.overlappings;
    }

    private int[] values() {
        return Arrays.stream(vars).mapToInt(IntExp::max).toArray();
    }

    /**
     * An input and the rules that cover it.
     */
    @RequiredArgsConstructor
    private static final class Hit {
        private final int[] values;
        private final int[] rules;
    }

    /**
     * Two rules, the one with the smaller index goes first.
     */
    private record RulePair(int first, int second) {
    }

    /**
     * The search for the overlapping rules.
     * <p>
     * It finds an input that two or more rules cover, and checks how each pair of these rules overlaps. Then it
     * removes either rule of the pair and searches again, to find the overlappings that the pair hides.
     */
    private final class OverlappingSearch {

        private final List<Overlapping> overlappings = new ArrayList<>();
        private final Set<RulePair> checkedPairs = new HashSet<>();
        private final boolean[] removed = new boolean[rules.length];
        private final boolean[] hadBeenRemoved = new boolean[rules.length];

        void check() {
            if (overlappings.size() > MAX_OVERLAPPINGS) {
                return;
            }
            var hit = findHit();
            if (hit != null) {
                checkPairs(hit);
            }
        }

        /**
         * Finds the input that two or more rules cover, the removed rules aside.
         */
        private @Nullable Hit findHit() {
            var remaining = IntStream.range(0, rules.length)
                    .filter(i -> !removed[i])
                    .mapToObj(i -> rules[i])
                    .toArray(IntExp[]::new);
            var hits = new ArrayList<Hit>(1);
            constrainer.solve(() -> IntExpAddArray.sum(constrainer, remaining).gt(1).equalTo(1), vars, () -> {
                var covering = IntStream.range(0, rules.length)
                        .filter(i -> !removed[i] && rules[i].bound() && rules[i].max() == 1)
                        .toArray();
                hits.add(new Hit(values(), covering));
                return null;
            });
            return hits.isEmpty() ? null : hits.getFirst();
        }

        private void checkPairs(Hit hit) {
            for (var i = 0; i < hit.rules.length; i++) {
                for (var j = i + 1; j < hit.rules.length; j++) {
                    checkPair(hit.values, hit.rules[i], hit.rules[j]);
                }
            }
        }

        private void checkPair(int[] values, int first, int second) {
            if (!checkedPairs.add(new RulePair(first, second))) {
                return;
            }
            var a = overrideAscending ? first : second;
            var b = overrideAscending ? second : first;
            overlappings.add(new Overlapping(new int[]{a, b}, status(rules[a], rules[b]), names, values));
            checkWithout(a);
            checkWithout(b);
        }

        /**
         * Returns how the rules overlap, where the rule {@code a} is applied first.
         */
        private OverlappingStatus status(IntBoolExp a, IntBoolExp b) {
            if (covers(a, b)) {
                return BLOCK;
            }
            if (covers(b, a)) {
                return OVERRIDE;
            }
            return PARTIAL;
        }

        /**
         * Returns whether the rule covers every input that the other rule covers.
         */
        private boolean covers(IntBoolExp rule, IntBoolExp other) {
            return !constrainer.solve(() -> rule.lt(other).equalTo(1), vars, () -> null);
        }

        /**
         * Searches again without the rule, unless it has already been done.
         */
        private void checkWithout(int rule) {
            if (hadBeenRemoved[rule]) {
                return;
            }
            hadBeenRemoved[rule] = true;
            removed[rule] = true;
            check();
            removed[rule] = false;
        }
    }
}
