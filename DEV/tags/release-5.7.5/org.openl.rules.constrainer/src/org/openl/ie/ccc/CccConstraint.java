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
/**
 * Class CccConstraint.
 * Creation date: (2/10/2000 11:54:58 AM)
 * @author:
 */

import org.openl.ie.constrainer.FloatVar;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.lpsolver.ConstrainerLP;

public class CccConstraint extends CccExecutable {
    private FloatVar _violation_var;
    private double _importance;
    private ConstrainerLP simplex = null;
    private IntBoolExp boolExp = null;

    public CccConstraint(CccCore core, String name) throws Exception {
        super(core, name);
        _violation_var = core.constrainer().addFloatVar(0.0, 1.0);
        _importance = 1.0;
        setType(TM_CONSTRAINT);
        status(STATUS_INACTIVE);
    }

    @Override
    synchronized public boolean activate(int solution_number) {
        return core().activateConstraint(getId());
    }

    @Override
    synchronized public void deactivate() {
        core().deactivateConstraint(getId());
    }

    @Override
    public void fetchConstrainerState() {
        if (core().isActivated(getId())) {
            if (core().isIncompatible(getId())) {
                status(STATUS_INCOMPATIBLE);
            } else {
                status(STATUS_ACTIVE);
            }
        } else {
            status(STATUS_INACTIVE);
        }
    }

    public IntBoolExp getBoolExp() {
        return boolExp;
    }

    public ConstrainerLP getSimplex() {
        return simplex;
    }

    public double importance() {
        return _importance;
    }

    public void importance(double i) {
        _importance = i;
    }

    /**
     * @param exp
     */
    public void setBoolExp(IntBoolExp exp) {
        boolExp = exp;
    }

    public void setSimplex(ConstrainerLP constrainerLP) {
        simplex = constrainerLP;
    }

    public FloatVar violationVar() {
        return _violation_var;
    }

}
