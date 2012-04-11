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
/**
 * Class CccFloat.
 *
 */
import org.openl.ie.constrainer.FloatExp;
import org.openl.ie.constrainer.GoalFloatMinimize;
import org.openl.util.Log;


public class CccFloat extends CccVariable {
    private double _min;
    private double _max;
    private FloatExp _constrainer_float;
    private String _value;
    private CccGoal _goal_minimize;
    private CccGoal _goal_maximize;

    /**
     * CccFloat constructor comment.
     */
    public CccFloat(CccCore core, double min, double max, String name) {
        super(core, TM_FLOAT, name);
        _min = min;
        _max = max;
        _value = "[" + min + ";" + max + "]";
        try {
            _constrainer_float = core.constrainer().addFloatVar(min, max, name);
        } catch (Exception e) {
            Log.error("Invalid min/max: " + e);
        }
    }

    public CccFloat(CccCore core, FloatExp i) {
        this(core, i, i.name());
    }

    public CccFloat(CccCore core, FloatExp i, String newname) {
        super(core, TM_FLOAT, newname);
        _constrainer_float = i;
        fetchConstrainerState();
    }

    public FloatExp constrainerFloat() {
        return _constrainer_float;
    }

    public void constrainerFloat(FloatExp var) {
        _constrainer_float = var;
    }

    @Override
    public String debugInfo() {
        return "observers=" + _constrainer_float.observers().size();
    }

    @Override
    public void fetchConstrainerState() {
        min(round(_constrainer_float.min()));
        max(round(_constrainer_float.max()));
        bound(_constrainer_float.bound());
        if (bound()) {
            status(STATUS_GREEN);
            value("[" + min() + "]");
        } else {
            status(STATUS_UNKNOWN);
            value("[" + round(_constrainer_float.min()) + ";" + round(_constrainer_float.max()) + "]");
        }
        // System.out.println( name()+"::fetchConstrainerState() -> "+value() );
    }

    @Override
    public CccGoal getMaximizeGoal() {
        if (_goal_maximize == null) {
            _goal_maximize = new CccGoal(core(), "MAXIMIZE");
            _goal_maximize.executable(new GoalFloatMinimize(core().getGoalSolution().executable(), _constrainer_float
                    .neg()));
        }
        return _goal_maximize;
    }

    @Override
    public CccGoal getMinimizeGoal() {
        if (_goal_minimize == null) {
            _goal_minimize = new CccGoal(core(), "MINIMIZE");
            _goal_minimize.executable(new GoalFloatMinimize(core().getGoalSolution().executable(), _constrainer_float));
        }
        return _goal_minimize;
    }

    public double max() {
        return _max;
    }

    public void max(double m) {
        _max = m;
    }

    public double min() {
        return _min;
    }

    /**
     * Sets the minimal value
     *
     * @param m int
     */
    public void min(double m) {
        _min = m;
    }

    double round(double x) {
        return ((int) ((x + 0.005) * 100)) / 100.;

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
