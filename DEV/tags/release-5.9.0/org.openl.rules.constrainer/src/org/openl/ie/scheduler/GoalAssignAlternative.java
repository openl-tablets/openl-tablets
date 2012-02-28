package org.openl.ie.scheduler;

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
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;
import org.openl.ie.constrainer.GoalImpl;
import org.openl.ie.constrainer.GoalOr;

/**
 * This goal assigns a resource from set for specified job consuming specified
 * capacity (used by requirement constraints)
 *
 * @see GoalAssign
 * @see AlternativeResourceConstraint
 */
public class GoalAssignAlternative extends GoalImpl {
    private AlternativeResourceConstraint _requirement;
    private Resource[] _rset;
    // private Job _job;
    // private int _capacity;
    // private IntVar _capacityVar;
    boolean[] _possibleAssignment;

    public GoalAssignAlternative(AlternativeResourceConstraint req, boolean[] possibleAssgn) {
        super(req.constrainer(), "");
        _requirement = req;
        _rset = _requirement.resources();
        // _job = _requirement.getJob();
        // _rset = rset;
        // _capacity = capacity;
        // _capacityVar = capacityVar;
        _possibleAssignment = possibleAssgn;
        name("GoalAssignAlternative(" + _requirement.getJob() + "," + _rset + ")");
    }

    public Goal execute() throws Failure {

        Goal _goal = null;
        for (int i = 0; i < _rset.length; i++) {
            if (_possibleAssignment[i]) {
                if (_goal == null) {
                    _goal = new GoalAssign(_requirement, i);
                } else {
                    _goal = new GoalOr(_goal, new GoalAssign(_requirement, i));
                }
            }
        }

        _goal.execute();
        return null;
    }

}
