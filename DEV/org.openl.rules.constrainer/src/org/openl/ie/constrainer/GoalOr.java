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
 * An implementation of the OR-execution of several subgoals. The goal GoalOr succeeds when one subgoal succeed. It
 * fails when all subgoals fail.
 */
public class GoalOr extends GoalImpl {
    private Goal _g1;
    private Goal _g2;
    private ChoicePointLabel _label = null;

    /**
     * Constructor with a given 2 goals.
     */
    public GoalOr(Goal g1, Goal g2) {
        super(g1.constrainer(), "");// "{"+g1.name()+"||"+g2.name()+"}");
        _g1 = g1;
        _g2 = g2;
    }

    /**
     * Constructor with a given 2 goals and a label.
     */
    public GoalOr(Goal g1, Goal g2, ChoicePointLabel label) {
        super(g1.constrainer(), "");// "{"+g1.name()+"||"+g2.name()+"}");
        _g1 = g1;
        _g2 = g2;
        _label = label;
    }

    /**
     * Sets the choice point in the constrainer.
     */
    @Override
    public Goal execute() throws Failure {
        // Debug.print("Execute "+this);
        constrainer().setChoicePoint(_g1, _g2, _label);
        return null;
    }

    @Override
    public String toString() {
        return "{" + _g1 + "||" + _g2 + "}";
    }
}
