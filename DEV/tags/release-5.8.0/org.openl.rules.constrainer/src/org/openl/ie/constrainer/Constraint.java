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
 * The interface for constraints.
 * <p>
 * Any implementaion of constraint should:
 * <ul>
 * <li>save in its <b>constructor</b> the expressions on which this constraint
 * imposed.
 * <li>apply in <b>{@link #execute()}</b> the condition of this constraint to
 * the expressions.
 * </ul>
 *
 * @see Goal
 * @see ConstraintImpl
 */
public interface Constraint extends Goal {
    /**
     * Returns a constraint: <code>(this AND constraint)</code>. This
     * constraint is satisfied only when both the invoked constraint and the
     * parameter-constraint are satisfied. 'And' condition has straightforward
     * implementation for the constraints even in the minimal requirements to
     * the interface Constraint.
     */
    public Constraint and(Constraint constraint);

    public boolean isLinear();

    /**
     * Returns a constraint: <code>(NOT this)</code>. An opposite constraint
     * has semantically an opposite meaning to this constraint.
     */
    public Constraint opposite();

    // /**
    // * <b> * to be hidden * </b>
    // * Returns a constraint: <code>(this OR constraint)</code>.
    // * This constraint is satisfied only
    // * either the invoked constraint or the parameter-constraint is satisfied.
    // */
    // public Constraint or(Constraint constraint);
    //
    // /**
    // * <b> * to be hidden * </b>
    // * Returns a constraint satisfying the condition:
    // * if this constraint is true, then the "constraint" should be true.
    // */
    // public Constraint ifThen(Constraint constraint);
    //
    // /**
    // * <b> * to be hidden * </b>
    // * Returns a constraint satisfying the condition:
    // * if this "constraint" is true, then "constraint1" else "constraint2"
    // */
    // public Constraint ifThenElse(Constraint constraint1, Constraint
    // constraint2);

    /**
     * Makes the constraint active.
     *
     * @throws Failure if the constraint can not be satisfied.
     */
    public void post() throws Failure;

    public IntBoolExp toIntBoolExp();

}
