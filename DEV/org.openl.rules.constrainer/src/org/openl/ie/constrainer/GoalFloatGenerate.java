package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
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
import org.openl.ie.tools.FastVector;

/**
 * An implementation of a {@link Goal} that generates a solution for the problem with the unknown floating-point
 * variables.
 * <p>
 * This goal:
 * <ul>
 * <li>Selects the variable using {@link FloatVarSelector}.
 * <li>Works with the domain of the variable using or dichotomize algorithm.
 * </ul>
 */
public class GoalFloatGenerate extends GoalImpl {
    private FloatExpArray _vars;
    private FloatVarSelector _var_selector;

    /**
     * The search goals that instantiate each variable.
     */
    private FastVector _goals;

    /**
     * Constructor with a given array of variables.
     */
    public GoalFloatGenerate(FloatExpArray vars) {
        this(vars, null);
    }

    /**
     * Constructor with a given array of variables, and variable selector.
     */
    public GoalFloatGenerate(FloatExpArray vars, FloatVarSelector var_selector) {
        super(vars.constrainer(), "FloatGenerate");

        _vars = vars;

        _var_selector = (var_selector != null ? var_selector : new FloatVarSelectorFirstUnbound(vars));

        initGoals();
    }

    /**
     * Instantiates all variables in the order defined by the variable selector.
     */
    @Override
    public Goal execute() throws Failure {
        int index = _var_selector.select();

        if (index == -1) {
            return null; // all vars are instantiated
        }

        FloatExp var = _vars.elementAt(index);

        Goal search_goal = (Goal) _goals.elementAt(index);

        return new GoalAnd(search_goal, this);
    }

    /**
     * Initializes an array of instantiation goals for the variables.
     */
    void initGoals() {
        int size = _vars.size();
        _goals = new FastVector(size);
        for (int i = 0; i < size; i++) {
            FloatExp exp = _vars.elementAt(i);
            Goal goal = new GoalFloatInstantiate((FloatVar) exp);
            _goals.addElement(goal);
        }
    }

    /**
     * Returns a String representation of this goal.
     *
     * @return a String representation of this goal.
     */
    @Override
    public String toString() {
        return "FloatGenerate(" + _vars.size() + ")";
    }

    /**
     * Returns the array of the variables for this goal.
     */
    public FloatExpArray vars() {
        return _vars;
    }

} // ~GoalFloatGenerate
