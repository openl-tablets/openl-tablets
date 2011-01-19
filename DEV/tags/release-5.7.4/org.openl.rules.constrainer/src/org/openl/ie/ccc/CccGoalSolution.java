package org.openl.ie.ccc;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000, 2002
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
public class CccGoalSolution extends CccGoal {
    private int _solution_number;

    /**
     * CccSolution constructor comment.
     *
     * @param core org.openl.ie.ccc.CccCore
     */
    public CccGoalSolution(CccCore core) {
        super(core, "Solution");
        _is_solution = true;
        _solution_number = 1;
        setType(CccConst.TM_SOLUTION);
    }

    /**
     * Insert the method's description here. Creation date: (2/21/2000 2:18:39
     * PM)
     *
     * @return boolean
     * @param solution_number int
     */
    @Override
    public boolean activate(int solution_number) {

        return core().activateGoal(getId(), solution_number);
    }

    /**
     * Insert the method's description here. Creation date: (2/21/2000 2:16:53
     * PM)
     *
     * @return int
     */
    public int solutionNumber() {
        return _solution_number;
    }
}
