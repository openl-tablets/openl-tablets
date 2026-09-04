package org.openl.ie.constrainer.consistencyChecking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

public class OverlappingCheckerImpl2 implements OverlappingChecker {

    private static final int MAX_OVERLOADS = 50;
    private final CDecisionTable _dt;

    private final List<Overlapping> overlappings = new ArrayList<>();

    private final HashSet<IntPair> checkedPairs = new HashSet<>();

    private final boolean[] removed;
    private final boolean[] hadBeenRemoved;
    private int nRemoved;

    private void remove(int i) {
        removed[i] = true;
        nRemoved++;
    }

    private void restore(int i) {
        removed[i] = false;
        nRemoved--;
    }

    private class GoalSaveSolutions extends GoalImpl {

        private static final long serialVersionUID = 4298252562811799305L;

        private final List<Overlapping> overlappingRules;

        private GoalSaveSolutions(Constrainer c, List<Overlapping> ovlRules) {
            super(c);
            this.overlappingRules = ovlRules;
        }

        @Override
        public Goal execute() throws Failure {
            var over = new Overlapping(_dt.getVars());
            for (var i = 0; i < _dt.getRules().length; i++) {
                if (removed[i]) {
                    continue;
                }
                var rule = _dt.getRule(i);
                if (rule.bound() && rule.max() == 1) {
                    over.addRule(i);
                }
            }
            if (over.amount() > 0) {
                overlappingRules.add(over);
            }
            return null;
        }
    }

    public OverlappingCheckerImpl2(CDecisionTable _dt) {
        this._dt = _dt;
        removed = new boolean[_dt.getRules().length];
        hadBeenRemoved = new boolean[_dt.getRules().length];
    }

    public void checkInternal() {
        // User will not see all overloads if there is too many of them.
        // TODO Optimize algorithm and remove check for MAX_OVERLOADS. Added for EPBDS-5694
        if (overlappings.size() > MAX_OVERLOADS) {
            return;
        }

        var overlappingRules = new ArrayList<Overlapping>();
        var rules = _dt.getRules();
        var C = rules[0].constrainer();
        var stackSize = C.getStackSize();

        var ruleArray = new IntExpArray(C, rules.length - nRemoved);
        for (int i = 0, r = 0; i < rules.length; i++) {
            if (!removed[i]) {
                ruleArray.set(rules[i], r++);
            }
        }
        var overlapping = ruleArray.sum().gt(1).asConstraint();
        var save = new GoalSaveSolutions(C, overlappingRules);
        var generate = new GoalGenerate(_dt.getVars());
        var target = new GoalAnd(new GoalAnd(overlapping, generate), save);
        C.execute(target, true);
        C.backtrackStack(stackSize);

        testPairOverlappings(overlappingRules);
    }

    private void testPairOverlappings(List<Overlapping> overlappingRules) {

        for (Overlapping ovl : overlappingRules) {
            var rules = ovl.getOverlapped();

            for (var i = 0; i < rules.length; i++) {
                for (var j = i + 1; j < rules.length; j++) {

                    var pair = new IntPair(rules[i], rules[j]);
                    if (checkedPairs.contains(pair)) {
                        continue;
                    }
                    checkedPairs.add(pair);

                    int A = _dt.isOverrideAscending() ? i : j;
                    int B = _dt.isOverrideAscending() ? j : i;

                    if (completelyOverlaps(_dt.getRule(rules[A]), _dt.getRule(rules[B]))) {
                        this.overlappings
                                .add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.BLOCK));
                    } else if (completelyOverlaps(_dt.getRule(rules[B]), _dt.getRule(rules[A]))) {
                        this.overlappings
                                .add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.OVERRIDE));
                    } else /* if (!blocks && !overrides) */ {
                        this.overlappings
                                .add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.PARTIAL));
                    }
                    checkWithRemove(rules[A]);
                    checkWithRemove(rules[B]);
                }
            }

        }

    }

    private void checkWithRemove(int ind) {
        if (hadBeenRemoved[ind]) {
            return;
        }
        hadBeenRemoved[ind] = true;
        remove(ind);
        checkInternal();
        restore(ind);

    }

    private boolean completelyOverlaps(IntExp exp1, IntExp exp2) {
        var C = exp1.constrainer();
        var stackSize = C.getStackSize();
        var overlaps = exp1.lt(exp2).asConstraint();
        var generate = new GoalGenerate(_dt.getVars());
        var target = new GoalAnd(overlaps, generate);
        var flag = C.execute(target, true);
        C.backtrackStack(stackSize);
        return !flag;
    }

    @Override
    public List<Overlapping> check() {
        checkInternal();
        return overlappings;
    }

}
