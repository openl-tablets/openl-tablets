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
 * An implementation of {@link Goal} that execution generates {@link Failure}.
 */
public class GoalFail extends GoalImpl {
    private ChoicePointLabel _label = null;

    /**
     * Constructor with a given constrainer.
     */
    public GoalFail(Constrainer c) {
        super(c, "GoalFail");
    }

    /**
     * Constructor with a given constrainer and label.
     */
    public GoalFail(Constrainer c, ChoicePointLabel label) {
        super(c, "GoalFail");
        _label = label;
    }

    /**
     * Calls Constrainer's methods fail().
     */
    public Goal execute() throws Failure {
        constrainer().fail("Goal Fail", _label);
        return null;
    }
}
