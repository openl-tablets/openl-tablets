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
//: GoalImpl.java
//
/**
 * A generic abstract implementation of the {@link Goal} interface.
 * <p>
 * Any specific subclass of the GoalImpl:
 * <ul>
 * <li>Should implement the method {@link #execute()}.
 * </ul>
 *
 * @see ConstraintImpl
 */
public abstract class GoalImpl extends ConstrainerObjectImpl implements Goal {
    public GoalImpl(Constrainer c) {
        this(c, "Goal");
    }

    public GoalImpl(Constrainer c, String name) {
        super(c, name);
        // c.addGoal(this);
    }

    public boolean toContinue(ChoicePointLabel label, boolean restore) {
        return constrainer().toContinue(label, restore);
    }

} // ~GoalImpl
