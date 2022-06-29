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
 * An implementation of a {@link Goal} that generates a solution for the problem with the unknown integer variables.
 * <p>
 * This goal:
 * <ul>
 * <li>Tries to instantiate it using {@link GoalInstantiate}. GoalInstantiate make use of IntValueSelector taken from a
 * constructor of GoalGenerate to work with the domain of a variable.
 * </ul>
 *
 */
public class GoalGenerate extends GoalImpl {
    private final IntExpArray _intvars;

    /**
     * The search goals that instantiate each variable.
     */
    private FastVector _goals;

    /**
     * Constructor with a given array of variables. GoalGenerate will be constructed with
     * <code>IntVarSelectorFirstUnbound<code>
     * as a default variable selector and <code>IntValueSelectorMin</code> as a default value selector and it won't use
     * dichotomize.
     */
    public GoalGenerate(IntExpArray intvars) {
        super(intvars.constrainer(), "Generate");
        _intvars = intvars;

        initGoals();
    }

    /**
     * Instantiates all variables in the order defined by the IntVarSelector.
     */
    @Override
    public Goal execute() throws Failure {
        // Debug.on();Debug.print("Generate"+_intvars);Debug.off();
        int index = -1;
        int size = _intvars.size();
        for (int i = 0; i < size; i++) {
            IntVar vari = (IntVar) _intvars.elementAt(i);
            if (!vari.bound()) {
                index = i;
                break;
            }
        }
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
            goal = new GoalInstantiate(var);
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
