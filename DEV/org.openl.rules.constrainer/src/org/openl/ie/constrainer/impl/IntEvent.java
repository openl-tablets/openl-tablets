package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.EventOfInterest;
import org.openl.ie.constrainer.IntExp;

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
 * A delta-event about the change(s) in the domain of the constraint integer expression. This event contains an
 * information about current and old (before the change(s)) domains of the expression.
 */
public abstract class IntEvent extends EventOfInterest {
    /**
     * An expression for which this event is.
     */
    protected IntExp _exp;

    /**
     * Returns a String representation of the changes in the domain.
     */
    public String domainToString() {
        return " [" + oldmin() + "-" + min() + " ; " + max() + "-" + oldmax() + "]";
    }

    /**
     * Returns the expression for which this event is.
     */
    public IntExp exp() {
        return _exp;
    }

    /**
     * Sets the expression for which this event is.
     */
    public void exp(IntExp e) {
        _exp = e;
    }

    /**
     * Returns the current largest value of the domain of the expression.
     */
    public abstract int max();

    /**
     * Returns the delta-change of the largest value of the domain of the expression.
     */
    public int maxdiff() {
        return max() - oldmax();
    }

    /**
     * Returns the current smallest value of the domain of the expression.
     */
    public abstract int min();

    /**
     * Returns the delta-change of the smallest value of the domain of the expression.
     */
    public int mindiff() {
        return min() - oldmin();
    }

    /**
     * Returns number of removed values from the domain of the expression.
     *
     * @see #removed
     */
    public abstract int numberOfRemoves();

    /**
     * Returns the old largest value of the domain of the expression.
     */
    public abstract int oldmax();

    /**
     * Returns the old smallest value of the domain of the expression.
     */
    public abstract int oldmin();

    /**
     * Returns i<sup>th</sup> value that was removed from the domain of the expression.
     *
     * @see #numberOfRemoves
     */
    public abstract int removed(int i);

    /**
     * Returns a String representation of this event.
     *
     * @return a String representation of this event.
     */
    @Override
    public String toString() {
        return name() + "(" + _exp + " : " + maskToString() + domainToString() + ")";
    }

} // ~IntEvent
