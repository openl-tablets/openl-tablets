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
public class CccGoal extends CccExecutable {
    protected boolean _is_solution;

    public CccGoal(CccCore core, String name) {
        super(core, name);
        _is_solution = false;
        setType(TM_GOAL);
        status(STATUS_INACTIVE);
    }

    @Override
    synchronized public boolean activate(int solution_number) {
        core().traceln("->> CccGoal: activate(s)");
        core().activateGoal(getId(), solution_number);
        return true;
    }

    @Override
    synchronized public void deactivate() {
        core().deactivateGoal();
    }

    @Override
    public void fetchConstrainerState() {
        // if (core().isActivated( getId() ))
        // status(STATUS_ACTIVE);
    }

    public boolean isSolution() {
        return _is_solution;
    }

}
