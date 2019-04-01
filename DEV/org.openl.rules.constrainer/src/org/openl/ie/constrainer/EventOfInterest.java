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
import org.openl.ie.tools.ReusableImpl;

//: EventOfInterest.java
/**
 * Class EventOfInterest is a base class for different events used by constraint observers.
 *
 * @see Observer
 * @see Subject
 */
public abstract class EventOfInterest extends ReusableImpl implements EventOfInterestConstants {
    /**
     * The constants for the EventOfInterest.
     */
    public interface Constants extends EventOfInterestConstants {
    } // ~Constants

    /**
     * The names of the events.
     */
    static final String[] names = { "VALUE", "MIN", "MAX", "REMOVE" };

    /**
     * Returns true if this event is MAX event.
     */
    public boolean isMaxEvent() {
        return (type() & MAX) != 0;
    }

    /**
     * Returns true if this event is MIN event.
     */
    public boolean isMinEvent() {
        return (type() & MIN) != 0;
    }

    /**
     * Returns true if this event is REMOVE event.
     */
    public boolean isRemoveEvent() {
        return (type() & REMOVE) != 0;
    }

    /**
     * Returns true if this event is VALUE event.
     */
    public boolean isValueEvent() {
        return (type() & VALUE) != 0;
    }

    /**
     * Returns a String representation of the mask of this event.
     */
    public String maskToString() {
        String res = "(";
        boolean first = true;
        for (int i = 0; i < names.length; ++i) {
            if ((type() & (1 << i)) != 0) {
                if (!first) {
                    res += " | ";
                }
                res += names[i];
                first = false;
            }
        }
        res += ")";
        return res;
    }

    /**
     * Returns the name of this event.
     */
    public abstract String name();

    /**
     * Returns a String representation of this object.
     *
     * @return a String representation of this object.
     */
    @Override
    public String toString() {
        return name() + maskToString();
    }

    /**
     * Returns the type of this event.
     */
    public abstract int type();

} // ~EventOfInterest

/**
 * The constants for the EventOfInterest. Temporary is not inner interface of the EventOfInterest because of compilation
 * problem with javac.
 */
interface EventOfInterestConstants {
    /**
     * The mask for the VALUE event: when expression becomes bound.
     */
    int VALUE = 1;

    /**
     * The mask for the MIN event: when expression's minimum is changed.
     */
    int MIN = 2;

    /**
     * The mask for the MAX event: when expression's maximum is changed.
     */
    int MAX = 4;

    /**
     * The mask for the REMOVE event: when value(s) is(are) removed from the expression's domain.
     */
    int REMOVE = 8;

    /**
     * The mask for the MIN or MAX event.
     */
    int MINMAX = MIN | MAX;

    /**
     * The mask for ALL events.
     */
    int ALL = VALUE | MIN | MAX | REMOVE;

} // ~Constants
