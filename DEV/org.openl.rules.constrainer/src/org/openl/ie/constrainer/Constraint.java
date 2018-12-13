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
     * Makes the constraint active.
     *
     * @throws Failure if the constraint can not be satisfied.
     */
    public void post() throws Failure;

}
