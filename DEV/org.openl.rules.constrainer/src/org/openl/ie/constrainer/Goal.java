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
//
//: Goal.java
//
/**
 * An interface for the constrainer goal.
 *
 * @see Constraint
 * @see GoalImpl
 */
public interface Goal extends ConstrainerObject {
    /**
     * An implementation of the execution algorithm of this goal.
     *
     * Returns subgoal of this goal if there are any, null otherwise.
     */
    public Goal execute() throws Failure;

    public boolean toContinue(ChoicePointLabel label, boolean restore_flag);

} // ~Goal
