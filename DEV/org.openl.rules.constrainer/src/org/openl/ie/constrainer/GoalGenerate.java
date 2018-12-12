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
 * An implementation of a {@link Goal} that generates a solution for the problem
 * with the unknown integer variables.
 * <p>
 * This goal:
 * <ul>
 * <li>Selects the variable using {@link IntVarSelector}.
 * <li>Tries to instantiate it using {@link GoalInstantiate}. GoalInstantiate
 * make use of IntValueSelector taken from a constructor of GoalGenerate to work
 * with the domain of a variable.
 * </ul>
 *
 * @see IntVarSelector
 * @see IntValueSelector
 * @see GoalInstantiate
 */
public class GoalGenerate extends GoalImpl {
    private IntExpArray _intvars;
    private IntVarSelector _var_selector;
    private boolean _dichotomize;
    private IntValueSelector _value_selector;
    private boolean _recursiveInstantiate = true;

    /**
     * The search goals that instantiate each variable.
     */
    private FastVector _goals;

    /**
     * Constructor with a given array of variables. GoalGenerate will be
     * constructed with <code>IntVarSelectorFirstUnbound<code>
     * as a default variable selector and <code>IntValueSelectorMin</code> as a
     * default value selector and it won't use dichotomize.
     */
    public GoalGenerate(IntExpArray intvars) {
        this(intvars, null, null, false);
    }

    /**
     * GoalGenerate will be constructed with
     * <code>IntVarSelectorFirstUnbound<code>
     * as a default variable selector and <code>IntValueSelectorMin</code> as a
     * default value selector.
     */
    public GoalGenerate(IntExpArray intvars, boolean dichotomize) {
        this(intvars, null, null, dichotomize);
    }

    /**
     * Constructor with a given array of variables, variable selector, and
     * "dichotomize" parameter.
     */
    public GoalGenerate(IntExpArray intvars, IntVarSelector var_selector, boolean dichotomize) {
        this(intvars, var_selector, null, dichotomize);
    }

    /**
     * Constructor with a given array of variables, variable selector, and value
     * selector. GoalGenerate constructed that way won't use dichotomize
     * procedure.
     */
    public GoalGenerate(IntExpArray intvars, IntVarSelector var_selector, IntValueSelector value_selector) {
        this(intvars, var_selector, value_selector, false);
    }

    /**
     * Constructor with full (and redundant) set of parameters.
     */
    GoalGenerate(IntExpArray intvars, IntVarSelector var_selector, IntValueSelector value_selector, boolean dichotomize) {
        super(intvars.constrainer(), "Generate");
        _intvars = intvars;
        _dichotomize = dichotomize;

        _var_selector = (var_selector != null ? var_selector : new IntVarSelectorFirstUnbound(intvars));

        _value_selector = (dichotomize ? null : value_selector != null ? value_selector : new IntValueSelectorMin());
        initGoals();
    }

    /**
     * Instantiates all variables in the order defined by the IntVarSelector.
     */
    public Goal execute() throws Failure {
        // Debug.on();Debug.print("Generate"+_intvars);Debug.off();
        int index = _var_selector.select();
        if (index == -1) {
            return null; // all vars are instantiated
        }

        // IntVar var = (IntVar)_intvars.elementAt(index);
        // Debug.on();Debug.print("Var Selected:"+var);Debug.off();

        Goal search_goal = (Goal) _goals.elementAt(index);

        return new GoalAnd(search_goal, this);
    }

    /**
     * Initializes an array of instantiation goals for the variables.
     */
    void initGoals() {
        int size = _intvars.size();
        _goals = new FastVector(size);
        for (int i = 0; i < size; i++) {
            IntVar var = (IntVar) _intvars.elementAt(i);
            Goal goal;
            if (_dichotomize) {
                goal = new GoalDichotomize(var, _recursiveInstantiate);
            } else {
                goal = new GoalInstantiate(var, _value_selector, _recursiveInstantiate);
            }
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
        return "Generate(" + _intvars.size() + ")";
    }

    /**
     * Returns the array of the variables for this goal.
     */
    public IntExpArray vars() {
        return _intvars;
    }

} // ~GoalGenerate
