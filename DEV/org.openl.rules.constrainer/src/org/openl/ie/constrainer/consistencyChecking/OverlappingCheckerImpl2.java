package org.openl.ie.constrainer.consistencyChecking;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntExpArray;

public class OverlappingCheckerImpl2 implements OverlappingChecker {

    private CDecisionTable _dt = null;

    List<Overlapping> overlappings = new ArrayList<Overlapping>();
    
    HashSet<IntPair> checkedPairs = new HashSet<IntPair>();
    
    
    boolean[] removed;
    boolean[] hadBeenRemoved; 
    int nRemoved = 0;
    
    private void remove(int i)
    {
//        System.out.println(" ---- Remove " + i);
        
        removed[i] = true;
        nRemoved++;
    }
    
    private void restore(int i)
    {
//        System.out.println(" ++++ Restore " + i);
        removed[i] = false;
        nRemoved--;
    }

    private class GoalSaveSolutions extends GoalImpl {
        /**
             * 
             */
        private static final long serialVersionUID = 4298252562811799305L;

        List<Overlapping> overlappingRules;

        public GoalSaveSolutions(Constrainer c, List<Overlapping> ovlRules) {
            super(c);
            this.overlappingRules = ovlRules;
        }

        public Goal execute() throws Failure {
            Overlapping over = new Overlapping(_dt.getVars());
            for (int i = 0; i < _dt.getRules().length; i++) {
                if (removed[i])
                    continue;
                IntBoolExp rule = _dt.getRule(i);
                if (rule.bound() && (rule.max() == 1)) {
                    over.addRule(i);
                }
            }
            if (over.amount() > 0) {
                overlappingRules.add(over);
            }
            return null;
        }
    }

    private Constrainer C = null;

    public OverlappingCheckerImpl2(CDecisionTable _dt) {
        this._dt = _dt;
        removed = new boolean[_dt.getRules().length];
        hadBeenRemoved = new boolean[_dt.getRules().length];
    }

    public List<Overlapping> checkInternal() {

        List<Overlapping> overlappingRules = new ArrayList<Overlapping>();
        IntBoolExp[] rules = _dt.getRules();
        C = rules[0].constrainer();
        IntExpArray ruleArray = new IntExpArray(C, rules.length - nRemoved);
        for (int i = 0, r =0; i < rules.length; i++) {
            if (!removed[i])
                ruleArray.set(rules[i], r++);
        }
        Constraint overlapping = (ruleArray.sum().gt(1)).asConstraint();
        Goal save = new GoalSaveSolutions(C, overlappingRules);
        Goal generate = new GoalGenerate(_dt.getVars());
        Goal target = new GoalAnd(new GoalAnd(overlapping, generate), save);
        boolean flag = C.execute(target, true);
        testPairOverlappings(overlappingRules);
        return overlappingRules;
    }

    
    
    
    
    
    private void testPairOverlappings(List<Overlapping> overlappingRules) {

        for (int n = 0; n < overlappingRules.size(); n++) {
            Overlapping ovl = overlappingRules.get(n);
            int[] rules = ovl.getOverlapped();

            for (int i = 0; i < rules.length; i++) {
                for (int j = i + 1; j < rules.length; j++) {
                    
                    
                    IntPair pair = new IntPair(rules[i], rules[j]);
                    if (checkedPairs.contains(pair))
                        continue;
                    checkedPairs.add(pair);
                    
                    int A = _dt.isOverrideAscending() ? i : j;
                    int B = _dt.isOverrideAscending() ? j : i;
                    
                    boolean blocks = completelyOverlaps(_dt.getRule(rules[A]),_dt.getRule(rules[B]));

                    boolean overrides = completelyOverlaps(_dt.getRule(rules[B]),_dt.getRule(rules[A]));


                    if (blocks)
                    {    
//                        System.out.println(" +***+ Checking " + rules[A] + " vs " + rules[B] + " = blocks");
                        this.overlappings.add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.BLOCK));
                        checkWithRemove(rules[A]);
                        checkWithRemove(rules[B]);
                    }    
                    else if (overrides)
                    {    
//                        System.out.println(" +***+ Checking " + rules[A] + " vs " + rules[B] + " = overrides");
                        this.overlappings.add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.OVERRIDE));
                        checkWithRemove(rules[A]);
                        checkWithRemove(rules[B]);
                        
                    }    

                    else //if (!blocks && !overrides)
                    {    
//                        System.out.println(" +***+ Checking " + rules[A] + " vs " + rules[B] + " = partial overlap");
                        this.overlappings.add(new Overlapping(ovl, rules[A], rules[B], Overlapping.OverlappingStatus.PARTIAL));
                        checkWithRemove(rules[A]);
                        checkWithRemove(rules[B]);
                    }    

                }
            }

        }

    }
    
    private void checkWithRemove(int ind)
    {
        if (hadBeenRemoved[ind])
            return;
        hadBeenRemoved[ind] = true;
        remove(ind);
        checkInternal();
        restore(ind);
        
    }
    

    // private class GoalCompare extends GoalImpl
    // {
    //
    // Boolean result = null;
    // IntExp exp1, exp2;
    //
    // public GoalCompare(Constrainer c, IntExp exp1, IntExp exp2) {
    // super(c);
    // this.exp1 = exp1;
    // this.exp2 = exp2;
    // }
    //
    // /**
    // *
    // */
    // private static final long serialVersionUID = -1772201340972264887L;
    //
    // public Goal execute() throws Failure {
    //
    // result = exp1.bound() && exp2.bound();
    // return null;
    // }
    //
    // }

    /**
     * @return true if the first expression completely overlaps the second one.
     */

    private boolean completelyOverlaps(IntExp exp1, IntExp exp2) {
        C = exp1.constrainer();
        Constraint overlaps = (exp1.sub(exp2).lt(0)).asConstraint();
        // GoalCompare compare = new GoalCompare(C, exp1, exp2);
        Goal generate = new GoalGenerate(_dt.getVars());
        Goal target = new GoalAnd(overlaps, generate);
        boolean flag = C.execute(target, true);
        // boolean res = compare.result;
        return !flag;
    }

    public List<Overlapping> check() {
        checkInternal();
        return overlappings;
    }

}
