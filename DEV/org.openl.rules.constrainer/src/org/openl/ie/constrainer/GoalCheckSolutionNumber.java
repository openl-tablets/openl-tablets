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
 * An implementation of a {@link Goal} that checks the solution number and fails
 * if this number has not been reached.
 */
public class GoalCheckSolutionNumber extends GoalImpl {
    private int _solution_number;
    private int _current_solution;

    public GoalCheckSolutionNumber(Constrainer c, int solution_number) {
        super(c, "CheckSolutionNumber");
        _solution_number = solution_number;
        _current_solution = 0;
    }

    public Goal execute() throws Failure {
        _current_solution++;
        if (_current_solution < _solution_number) {
            _constrainer.fail("solution < " + _solution_number);
        }
        return null;
    }

    public int getCurrentSolutionNumber() {
        return _current_solution;
    }

    public void init(int solution_number) {
        _solution_number = solution_number;
        _current_solution = 0;
    }

    @Override
    public String toString() {
        return "GoalCheckSolutionNumber";
    }

} // ~GoalCheckSolutionNumber
