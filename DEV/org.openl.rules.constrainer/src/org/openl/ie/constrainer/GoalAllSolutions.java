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
 * An implementation of a {@link Goal} that finds all solutions using the same
 * search goal. This goal executes the "goal"-parameter to find a solution. If a
 * solution is found, the GoalAllSolutions fails (to mark the found solution as
 * "already tried"), and executes the "goal"-parameter again until the are no
 * pther solutions. It is the responsibility of the "goal"-parameter to save
 * each found solution (if necessary). Example of use:
 *
 * <pre>
 *
 * Constrainer C = new Constrainer(&quot;Constrainer Test&quot;);
 * IntVar x = C.addIntVar(0, 10, &quot;x&quot;);
 * IntVar y = C.addIntVar(0, 10, &quot;y&quot;);
 * IntVar z = C.addIntVar(0, 1000, &quot;z&quot;);
 * x.more(3).post();
 * x.less(y).post();
 * z.equals(C.sum(x, y)).post();
 * IntExpArray vars = new IntExpArray(C, x, y, z);
 * Goal solution = new GoalAnd(new GoalGenerate(vars), new GoalPrintSolutionNumber(C), new GoalPrint(vars));
 * Goal all_solutions = new GoalAllSolutions(solution);
 * C.execute(all_solutions);
 * </pre>
 */
public class GoalAllSolutions extends GoalImpl {
    private Goal _search_goal;
    private Goal _fail_goal;
    private Goal _execution_goal;

    /**
     * Constructor with a given search goal.
     */
    public GoalAllSolutions(Goal search_goal) {
        super(search_goal.constrainer(), "GenerateAllSolutions");
        _search_goal = search_goal;
        _fail_goal = new GoalFail(constrainer());
        _execution_goal = new GoalAnd(_search_goal, _fail_goal);
    }

    /**
     * Executes the goal-parameter and
     *
     * @return GoalAnd(search_goal,fail_goal).
     */
    public Goal execute() throws Failure {
        // Goal goal = new GoalAnd(_search_goal,_fail_goal);
        // return new GoalAnd(goal,this);
        return _execution_goal;
    }

    /**
     * @return a String representation of this goal.
     */
    @Override
    public String toString() {
        return "AllSolutions using " + _search_goal;
    }

} // ~GoalAllSolutions
