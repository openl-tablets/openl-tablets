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
 * An implementation of a {@link Goal} that instantiates a constrained integer
 * variable.
 * <p>
 * It recursively splits the domain of the variable into two parts. If the
 * variable is already bound, it does nothing and succeeds. Otherwise, the goal
 * sets a choice point, and replaces the domain by one of its halves, and (if
 * recursive) calls itself again. The goal execution will be stopped when the
 * variable is bound or when a failure occurs.
 *
 * @see GoalInstantiate
 */
public class GoalDichotomize extends GoalImpl {
    private IntVar _var;
    private boolean _recursive;

    public GoalDichotomize(IntVar var, boolean recursive) {
        super(var.constrainer(), "Dichotomize(" + var.name() + ")");
        _var = var;
        _recursive = recursive;
    }

    /**
     * An implementation of the dichotomize instantiation algorithm for the
     * integer variable.
     */
    public Goal execute() throws Failure {
        // Debug.on();Debug.print("Execute "+this+ " with " + _var);Debug.off();
        if (_var.bound()) {
            return null;
        }

        // determine middle value
        int min = _var.min();
        int max = _var.max();
        int mid = (min + max) / 2;
        if (mid == max) {
            mid = max - 1;
        }

        Goal _goal_min = new GoalSetMin(_var, mid + 1);
        Goal _goal_max = new GoalSetMax(_var, mid);

        Goal new_goal;
        if (_recursive) {
            new_goal = new GoalAnd(new GoalOr(_goal_min, _goal_max), this);
        } else {
            new_goal = new GoalOr(_goal_min, _goal_max);
        }
        return new_goal;
    }

} // ~GoalDichotomize
