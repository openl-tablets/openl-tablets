package org.openl.ie.ccc;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2002
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalAnd;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalMinimize;
import org.openl.ie.constrainer.IntExp;

public class CccInteger extends CccVariable {
    private int _min;
    private int _max;
    private IntExp _constrainer_integer;
    private String _value;
    private CccGoal _goal_minimize;
    private CccGoal _goal_maximize;

    /**
     * CccInteger constructor comment.
     */
    public CccInteger(CccCore core, int min, int max, String name) {
        super(core, TM_INT, name);
        _goal_minimize = null;
        _goal_maximize = null;
        try {
            _constrainer_integer = core.constrainer().addIntVar(min, max, name);
        } catch (Exception e) {
            core.traceln("Invalid min/max: " + e);
        }
        fetchConstrainerState();
    }

    public CccInteger(CccCore core, IntExp i) {
        this(core, i, i.name());
    }

    public CccInteger(CccCore core, IntExp i, String newname) {
        super(core, TM_INT, newname);
        _constrainer_integer = i;
        fetchConstrainerState();
    }

    public IntExp constrainerInteger() {
        return _constrainer_integer;
    }

    public void constrainerInteger(IntExp var) {
        _constrainer_integer = var;
    }

    @Override
    public String debugInfo() {
        return "observers=" + _constrainer_integer.observers().size() + "deps="
                + _constrainer_integer.allDependents().size();
    }

    @Override
    public void fetchConstrainerState() {
        value(_constrainer_integer.domainToString());
        bound(_constrainer_integer.bound());
        min(_constrainer_integer.min());
        max(_constrainer_integer.max());
        if (!bound()) {
            status(STATUS_UNKNOWN);
        } else {
            status(STATUS_GREEN);
        }
    }

    @Override
    public CccGoal getMaximizeGoal() {
        if (_goal_maximize == null) {
            _goal_maximize = new CccGoal(core(), "MAXIMIZE");
            _goal_maximize.executable(new GoalMinimize(core().getGoalSolution().executable(), _constrainer_integer
                    .neg()));
        }
        return _goal_maximize;
    }

    @Override
    public CccGoal getMinimizeGoal() {

        if (_goal_minimize == null) {
            // System.out.println("Creating goal minimize for silution:
            // "+core().solution().executable());
            _goal_minimize = new CccGoal(core(), "MINIMIZE");
            _goal_minimize.executable(new GoalAnd(new GoalImpl(core().constrainer()) {
                public Goal execute() {
                    core().traceln("Minimize STARTED!");
                    core().traceVars();
                    return null;
                }
            }, new GoalMinimize(core().getGoalSolution().executable(), _constrainer_integer), new GoalImpl(core()
                    .constrainer()) {
                public Goal execute() {
                    core().traceln("Minimize FINISHED!");
                    core().traceVars();
                    return null;
                }
            }));
        }
        return _goal_minimize;
    }

    public int max() {
        return _max;
    }

    public void max(int m) {
        _max = m;
    }

    public int min() {
        return _min;
    }

    public void min(int m) {
        _min = m;
    }

    @Override
    public String toString() {
        return (name() + value());
    }

    @Override
    public String value() {
        return _value;
    }

    public void value(String v) {
        _value = v;
    }

}
