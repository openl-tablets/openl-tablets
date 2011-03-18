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
import org.openl.ie.constrainer.IntExp;
import org.openl.ie.constrainer.IntVar;
import org.openl.util.Log;


/**
 * This goal assigns specified resource for specified job consuming specified
 * capacity (used by requirement constraints)
 *
 * @see GoalAssignAlternative
 */
public class GoalAssign extends GoalImpl {
    private Resource _resource;
    private Job _job;
    private int _capacity;
    private IntVar _capacityVar;
    private AlternativeResourceConstraint _requirement;

    public GoalAssign(AlternativeResourceConstraint req, int resourceNum) {
        super(req.constrainer(), "");
        _requirement = req;
        _job = _requirement.getJob();
        _resource = _requirement.getResource(resourceNum);
        _capacity = _requirement.getCapacity();
        _capacityVar = _requirement.getCapacityVar();
        name("GoalAssign(" + _job + "," + _resource + ")");
    }

    private boolean apply() throws Failure {
        int c_start = _resource.timeMin() < _job.startMin() ? _job.startMin() : _resource.timeMin();
        int c_end = _resource.timeMax() > _job.endMax() ? _job.endMax() : _resource.timeMax();

        if (c_start > c_end) {
            String msg = "Resource " + _resource.getName() + " and Job " + _job.getName() + " do not overlap";
            Log.error(this + " Failure: " + msg);
            constrainer().fail(msg);
        }

        if (!_job.bound()) {
            constrainer().fail("Cannot assign unbound job " + _job);
            // return false;
        }
        if (_capacityVar != null) {
            if (!_capacityVar.bound()) {
                constrainer().fail("Cannot assign unbound job " + _job + " : capacity var is not bound");
            } else {
                _capacity = _capacityVar.value();
            }
        }
        /*
         * System.out.print("*** Applying RC("+_resource+","+_capacity+") on
         * "+_job+": ["); for(int t=c_start; t < c_end; t++) { IntExp capvar =
         * _resource.getCapacityVar(t); System.out.print(" "+capvar.max()); }
         * System.out.println(" ]");
         */
        for (int t = c_start; t < c_end; t++) {
            IntExp capvar = _resource.getCapacityVar(t);
            if (capvar.max() < _capacity) {
                constrainer().fail("resource assigning");
            }
        }
        for (int t = c_start; t < c_end; t++) {
            IntExp capvar = _resource.getCapacityVar(t);
            capvar.setMax(capvar.max() - _capacity);
        }
        // _job.assignResource(_resource);
        return true;
    }

    public Goal execute() throws Failure {
        try {
            // System.out.println("APP:"+this);
            apply();

        } catch (Failure f) {

            // System.out.println("FAIL");
            constrainer().fail("Unable to bind job " + _job + " to resource " + _resource);
            _job.saveAssignmentInfo();
        }

        if (_requirement != null) {
            _job.saveAssignmentInfo();
        }
        _requirement.assignResource(_resource);

        return null;
    }

}
