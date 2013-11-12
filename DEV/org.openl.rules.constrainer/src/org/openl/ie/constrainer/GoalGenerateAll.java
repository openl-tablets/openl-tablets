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

/**
 * An implementation of a {@link Goal} that finds and prints all solutions for
 * the problem with the unknown integer valiables.
 * <p>
 * This goal:
 * <ul>
 * <li>Selects the variable using {@link IntVarSelector}.
 * <li>Works with the domain of the variable using {@link IntValueSelector} or
 * dichotomize algorithm.
 * </ul>
 */
public class GoalGenerateAll extends GoalImpl {
    private Goal _search_goal;
    private Goal _print_goal;
    private Goal _fail_goal;

    /**
     * Constructor with a given {@link GoalGenerate}.
     */
    public GoalGenerateAll(GoalGenerate search_goal) {
        super(search_goal.constrainer(), "GenerateAll");
        _search_goal = search_goal;
        _print_goal = new GoalPrintSolution(search_goal.vars());
        _fail_goal = new GoalFail(constrainer());
    }

    /**
     * Constructor with a given array of variables. GoalGenerate will be
     * constructed with <code>IntVarSelectorFirstUnbound<code>
     * as a default variable selector and <code>IntValueSelectorMin</code> as a
     * default value selector and it won't use dichotomize procedure.
     */
    public GoalGenerateAll(IntExpArray intvars) {
        this(intvars, null, null, false);
    }

    /**
     * Constructor with a given array of variables and "dichotomize" parameter.
     * GoalGenerate will be constructed with
     * <code>IntVarSelectorFirstUnbound<code>
     * as a default variable selector and <code>IntValueSelectorMin</code> as a
     * default value selector.
     */
    public GoalGenerateAll(IntExpArray intvars, boolean dichotomize) {
        this(intvars, null, null, dichotomize);
    }

    /**
     * Constructor with a given array of variables, variable selector, and
     * "dichotomize" parameter.
     */
    public GoalGenerateAll(IntExpArray intvars, IntVarSelector var_selector, boolean dichotomize) {
        this(intvars, var_selector, null, dichotomize);
    }

    /**
     * Constructor with a given array of variables, variable selector, and value
     * selector.
     */
    public GoalGenerateAll(IntExpArray intvars, IntVarSelector var_selector, IntValueSelector value_selector) {
        this(intvars, var_selector, value_selector, false);
    }

    /**
     * Constructor with full (and redundant) set of parameters.
     */
    GoalGenerateAll(IntExpArray intvars, IntVarSelector var_selector, IntValueSelector value_selector,
            boolean dichotomize) {
        this(new GoalGenerate(intvars, var_selector, value_selector, dichotomize));
    }

    /**
     * Instantiates all variables in the order defined by the IntVarSelector.
     */
    public Goal execute() throws Failure {
        Goal goal = new GoalAnd(_search_goal, _print_goal, _fail_goal);
        return goal;
    }

    /**
     * Returns a String representation of this goal.
     *
     * @return a String representation of this goal.
     */
    @Override
    public String toString() {
        return "GenerateAll(" + _search_goal + ")";
    }

} // ~GoalGenerateAll
