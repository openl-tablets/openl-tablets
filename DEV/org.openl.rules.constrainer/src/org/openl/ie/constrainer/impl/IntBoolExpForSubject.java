package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.UndoableInt;

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
 * An abstract implementation of the IntBoolExp that is based on some boolean subject.
 */
public abstract class IntBoolExpForSubject extends IntBoolVarImpl {
    protected UndoableInt _subjectMin, _subjectMax;

    /**
     * Constructor with a given constrainer.
     */
    public IntBoolExpForSubject(Constrainer c) {
        this(c, "");
    }

    /**
     * Constructor with a given constrainer and name.
     */
    public IntBoolExpForSubject(Constrainer c, String name) {
        super(c, name);
    }

    /**
     * Returns true if the expression's is subject false. Note: this is not equals to <code>!isSubjectTrue()</code>.
     */
    abstract protected boolean isSubjectFalse();

    /**
     * Returns true if the expression's subject is true. Note: this is not equals to <code>!isSubjectFalse()</code>.
     */
    abstract protected boolean isSubjectTrue();

    protected final void setDomainMax(int max) throws Failure {
        super.setMax(max);
    }

    protected final void setDomainMin(int min) throws Failure {
        super.setMin(min);
    }

    /**
     * Sets the domain of this expression based on isSubjectTrue()/isSubjectFalse().
     */
    protected void setDomainMinMax() throws Failure {
        // if(_subjectMin.value() > _subjectMax.value())
        // abort("_subjectMin.value() > _subjectMax.value() in " + this);

        if (_subjectMin.value() == _subjectMax.value()) {
            return;
        }

        // Recalculate domain.
        if (isSubjectFalse()) {
            _subjectMax.setValue(0);
        } else if (isSubjectTrue()) {
            _subjectMin.setValue(1);
        }

        // if(_subjectMin.value() > _subjectMax.value())
        // abort("_subjectMin.value() > _subjectMax.value() in " + this);

        // Subject is bound -> update constraint domain.
        if (_subjectMin.value() == _subjectMax.value()) {
            super.setValue(_subjectMin.value());
        }
        // Subject is not bound -> propagate constraint domain.
        else {
            if (isTrue()) {
                // System.out.println("setDomainMinMax(): setSubjectTrue():
                // "+this);
                setSubjectTrue();
            } else if (isFalse()) {
                // System.out.println("setDomainMinMax(): setSubjectFalse():
                // "+this);
                setSubjectFalse();
            }
        }

    }

    /**
     * Sets the domain of this expression based on isSubjectTrue()/isSubjectFalse(). Should be called from constructor
     * to avoid failures. Note: doesn't use setMin/setMax because ctor should set _min/_max directly.
     */
    protected void setDomainMinMaxSafe() {
        if (isSubjectTrue()) {
            _min = 1;
        } else if (isSubjectFalse()) {
            _max = 0;
        }

        if (_min == _max) {
            _subjectMin = _subjectMax = constrainer().addUndoableInt(_min);
        } else {
            _subjectMin = constrainer().addUndoableInt(_min);
            _subjectMax = constrainer().addUndoableInt(_max);
        }
    }

    @Override
    public void setMax(int max) throws Failure {
        // System.out.println("+++IntBoolExpForSubject.setMax(" +max + ") in " +
        // this);

        if (max >= max()) {
            return;
        }

        super.setMax(max);

        // if(max < 0)
        // constrainer().fail("Constraint.setMax() < 0");

        // max==0 -> setFalse
        setSubjectFalse();

        // System.out.println("---IntBoolExpForSubject.setMax(" +max + ") in " +
        // this);
    }

    @Override
    public void setMin(int min) throws Failure {
        // System.out.println("+++IntBoolExpForSubject.setMin(" +min + ") in " +
        // this);

        if (min <= min()) {
            return;
        }

        super.setMin(min);

        // if(min > 1)
        // constrainer().fail("Constraint.setMin() > 1");

        // min==1 -> setTrue
        setSubjectTrue();

        // System.out.println("---IntBoolExpForSubject.setMin(" +min + ") in " +
        // this);
    }

    /**
     * Sets the value for the subject to false.
     */
    abstract protected void setSubjectFalse() throws Failure;

    /**
     * Sets the value for the subject to true.
     */
    abstract protected void setSubjectTrue() throws Failure;

} // ~IntBoolExpForSubject
