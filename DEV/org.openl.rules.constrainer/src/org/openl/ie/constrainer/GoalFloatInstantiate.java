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
 * An implementation of a {@link Goal} that instaintiates the constraint floating-point variable.
 * <p>
 * It recursively splits the domain of the variable into two parts. If the variable is already bound, it does nothing
 * and succeeds. Otherwise, the goal sets a choice point, and replaces the domain by one of its halves, and calls itself
 * again. The goal execution will be stopped when the variable is bound or when a failure occurs.
 *
 * @see FloatVar
 */
public class GoalFloatInstantiate extends GoalImpl {
    private FloatVar _var;

    public GoalFloatInstantiate(FloatVar var) {
        super(var.constrainer(), "Instantiate(" + var.name() + ")");
        _var = var;
    }

    /**
     * An implementation of the dichotomize instantiation algorithm for the floating-point variable.
     */
    public Goal execute() throws Failure {
        // Debug.on();Debug.print("Execute "+this+ " with " + _var);Debug.off();
        if (_var.bound()) {
            return null;
        }
        double mid_value = (_var.min() + _var.max()) / 2;
        // Debug.on();Debug.print("Try "+mid_value);Debug.off();
        Goal new_goal = new GoalOr(new GoalAnd(_var.lessOrEqual(mid_value), this),
            new GoalAnd(_var.moreOrEqual(mid_value), this));
        return new_goal;
    }
}
