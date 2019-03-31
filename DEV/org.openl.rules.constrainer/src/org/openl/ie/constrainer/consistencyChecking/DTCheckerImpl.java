package org.openl.ie.constrainer.consistencyChecking;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */
import java.util.ArrayList;
import java.util.List;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalGenerate;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;
import org.openl.ie.constrainer.IntExpArray;
import org.openl.ie.constrainer.IntVar;

public class DTCheckerImpl implements DTChecker {
    static public class CDecisionTableImpl implements CDecisionTable {
        private IntBoolExp[][] _data = null;
        private IntBoolExp[] _rules = null;
        private IntExpArray _vars = null;
        boolean overrideAscending;

        public CDecisionTableImpl(IntBoolExp[][] data, IntExpArray vars, boolean overrideAscending) {
            if (data == null) {
                throw new IllegalArgumentException(
                    "DecisionTableImpl(IntBoolExp[][] _data, IntExpArray vars) : can't be created based on null data array");
            }
            _data = data;
            _vars = vars;
            this.overrideAscending = overrideAscending;
            int nbRules = _data.length;
            _rules = new IntBoolExp[nbRules];
            java.util.Arrays.fill(_rules, new IntBoolExpConst(_vars.constrainer(), true));
            for (int i = 0; i < _data.length; i++) {
                int nbVars = _data[i].length;
                for (int j = 0; j < nbVars; j++) {
                    _rules[i] = _rules[i].and(_data[i][j]);
                }
            }
        }

        @Override
        public IntBoolExp getRule(int i) {
            return _rules[i];
        }

        @Override
        public IntBoolExp[] getRules() {
            return _rules;
        }

        @Override
        public IntVar getVar(int i) {
            return (IntVar) _vars.get(i);
        }

        @Override
        public IntExpArray getVars() {
            return _vars;
        }

        @Override
        public boolean isOverrideAscending() {
            return overrideAscending;
        }
    }

    private class CompletenessCheckerImpl implements CompletenessChecker {
        private class GoalSaveSolutions extends GoalImpl {
            private static final long serialVersionUID = -4747909482843265994L;

            public GoalSaveSolutions(Constrainer c) {
                super(c);
            }

            @Override
            public Goal execute() throws Failure {
                _uncoveredRegions.add(new Uncovered(_dt.getVars()));
                return null;
            }
        }

        private Constrainer C = null;

        @Override
        public List<Uncovered> check() {
            IntBoolExp[] rules = _dt.getRules();
            C = rules[0].constrainer();
            int stackSize = C.getStackSize();
            IntExpArray ruleArray = new IntExpArray(C, rules.length);
            for (int i = 0; i < rules.length; i++) {
                ruleArray.set(rules[i], i);
            }
            Constraint incompleteness = ruleArray.sum().equals(0);
            Goal save = new GoalSaveSolutions(C);
            Goal generate = new GoalGenerate(_dt.getVars());
            Goal target = new GoalAnd(new GoalAnd(incompleteness, generate), save);
            C.execute(target, true);
            C.backtrackStack(stackSize);
            return _uncoveredRegions;
        }
    }

    private CDecisionTable _dt = null;
    private CompletenessChecker _cpChecker = new CompletenessCheckerImpl();

    private OverlappingChecker _opChecker; // = new OverlappingCheckerImpl();

    private List<Uncovered> _uncoveredRegions = new ArrayList<Uncovered>();

    private List<Overlapping> _overlappingRules = new ArrayList<Overlapping>();

    public DTCheckerImpl(CDecisionTable dtable) {
        _dt = dtable;
        _opChecker = new OverlappingCheckerImpl2(_dt);
    }

    @Override
    public List<Uncovered> checkCompleteness() {
        return _cpChecker.check();
    }

    @Override
    public List<Overlapping> checkOverlappings() {
        return _opChecker.check();
    }

    @Override
    public CDecisionTable getDT() {
        return _dt;
    }

    @Override
    public void setDT(CDecisionTable dtable) {
        _dt = dtable;
    }

}