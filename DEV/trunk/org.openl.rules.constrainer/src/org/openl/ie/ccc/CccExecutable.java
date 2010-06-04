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
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.Goal;

public abstract class CccExecutable extends CccObject {
    // private boolean _fail = false;
    protected Goal _executable;

    // protected GoalCheckSolutionNumber _check_solution_number;

    public CccExecutable(CccCore core, String name) {
        super(core, name);
        _executable = null;
        // _check_solution_number = new
        // GoalCheckSolutionNumber(core.constrainer(),1);
    }

    public boolean activate() {
        // System.out.println("->> CccExecutable: activate()");

        return activate(1);
    }

    public abstract boolean activate(int solution_number);

    public abstract void deactivate();

    /*
     * public boolean getFail() { return _fail; } public void setFail(boolean b) {
     * //System.out.println("->> CccExecutable: fail("+b+"): "+this+"
     * "+_activated); _fail = b; }
     */
    public Goal executable() {
        return _executable;
    }

    public void executable(Goal goal) {
        _executable = goal;
    }

    public void execute() throws Failure {
        executable().execute();
    }

    public void fetchConstrainerState() {
    }
}
