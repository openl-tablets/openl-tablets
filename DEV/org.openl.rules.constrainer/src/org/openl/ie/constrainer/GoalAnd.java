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
 * A goal can be defined in terms of it's subgoals. <code>Class GoalAnd</code> makes it possible to create a goal
 * composed of a sequence (up to six) of other goals. To overcome the restriction of six goals imposed by it's
 * constructors one can use the following trick: let's assume the goalsArray to be an array of goals
 *
 * <pre>
 * Goal tempGoal = new GoalAnd(goalsArray.get(0),goalsArray.get(1));
 * for (int i = 2; i &amp;lt goalsArray.size(); i++){
 *    tempGoal = new GoalAnd(tempGoal, goalsArray.get(i));
 * }
 * </pre>
 *
 * GoalAnd pushes all it's subgoals to the execution stack. It succeeds if and only if all it's subgoals succeed. In
 * that case it returns null otherwise it generates an exception of type Failure.
 *
 */
public class GoalAnd extends GoalImpl {
    private Goal _g1;
    private Goal _g2;

    /**
     * Constructor of two arguments
     */
    public GoalAnd(Goal g1, Goal g2) {
        super(g1.constrainer(), "");// "{"+g1.name()+"&&"+g2.name()+"}");
        _g1 = g1;
        _g2 = g2;
    }

    /**
     * Constructor of two arguments
     */
    public GoalAnd(Goal g1, Goal g2, Goal g3) {
        this(g1, new GoalAnd(g2, g3));
    }

    /**
     * Constructor of two arguments
     */
    public GoalAnd(Goal g1, Goal g2, Goal g3, Goal g4) {
        this(g1, new GoalAnd(g2, g3, g4));
    }

    /**
     * Constructor of two arguments
     */
    public GoalAnd(Goal g1, Goal g2, Goal g3, Goal g4, Goal g5) {
        this(g1, new GoalAnd(g2, g3, g4, g5));
    }

    /**
     * Constructor of two arguments
     */
    public GoalAnd(Goal g1, Goal g2, Goal g3, Goal g4, Goal g5, Goal g6) {
        this(g1, new GoalAnd(g2, g3, g4, g5, g6));
    }

    /**
     * Executes this goal.
     *
     * @return Null if succeeds
     * @throws Failure if one of it's subgoals fails
     */
    public Goal execute() throws Failure {
        // Debug.print("Execute "+this);
        constrainer().pushOnExecutionStack(_g2);
        constrainer().pushOnExecutionStack(_g1);
        return null;
    }

    @Override
    public String toString() {
        return "{" + _g1 + "&&" + _g2 + "}";
    }

}
