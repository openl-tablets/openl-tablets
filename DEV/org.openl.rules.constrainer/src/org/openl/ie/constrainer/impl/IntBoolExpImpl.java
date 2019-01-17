package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Constraint;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.IntBoolExp;
import org.openl.ie.constrainer.IntBoolExpConst;

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
 * A generic implementation of the IntBoolExp interface.
 */
abstract public class IntBoolExpImpl extends IntExpImpl implements IntBoolExp {
    public IntBoolExpImpl(Constrainer c) {
        this(c, "");
    }

    public IntBoolExpImpl(Constrainer c, String name) {
        super(c, name);
    }

    public IntBoolExp and(boolean value) {
        return value ? (IntBoolExp) this : IntBoolExpConst.getIntBoolExpConst(constrainer(), false);
    }

    public IntBoolExp and(IntBoolExp exp) {
        // return new IntBoolExpAnd(this, exp);
        return getIntBoolExp(IntBoolExpAnd.class, this, exp);
    }

    public Constraint asConstraint() {
        return this.equals(1);
    }

    public boolean isFalse() {
        return max() == 0;
    }

    public boolean isTrue() {
        return min() == 1;
    }

    public IntBoolExp or(boolean value) {
        return value ? (IntBoolExp) IntBoolExpConst.getIntBoolExpConst(constrainer(), true) : this;
    }

    public IntBoolExp or(IntBoolExp exp) {
        // return new IntBoolExpOr(this, exp);
        return getIntBoolExp(IntBoolExpOr.class, this, exp);
    }

    public void setFalse() throws Failure {
        setMax(0);
    }

    public void setTrue() throws Failure {
        setMin(1);
    }

} // ~IntBoolExpImpl
