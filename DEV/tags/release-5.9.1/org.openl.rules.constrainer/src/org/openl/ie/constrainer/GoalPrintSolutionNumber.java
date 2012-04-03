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
 * An implementation of a {@link Goal} that prints the solution numbers.
 *
 * This goal is used with GoalAllSolutions to print the solution numbers.
 *
 * @see GoalAllSolutions
 */
public class GoalPrintSolutionNumber extends GoalImpl {
    private int _solution;

    /**
     * Creates GoalPrintSolutionNumber
     *
     * @param constrainer A Constrainer the goal belongs to.
     */
    public GoalPrintSolutionNumber(Constrainer constrainer) {
        super(constrainer, "PrintSolution");
        _solution = 0;
    }

    /**
     * Prints a solutions number to the output stream.
     *
     * @return null if succeeded
     * @throws Failure
     */
    public Goal execute() throws Failure {
        ++_solution;
        System.out.print("\nSolution " + _solution);
        return null;
    }

    /**
     * Returns a String representation of this goal.
     *
     * @return a String representation of this goal.
     */
    @Override
    public String toString() {
        return "PrintSolutionNumber";
    }

} // ~GoalPrintSolutionNumber
