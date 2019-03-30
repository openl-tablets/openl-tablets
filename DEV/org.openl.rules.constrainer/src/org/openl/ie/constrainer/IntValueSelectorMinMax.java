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
/**
 * An implementation of the IntValueSelector interface that interchanges the selection of the minimal and maximal values
 * from the domain of the variable.
 *
 * @see GoalInstantiate
 * @see GoalGenerate
 * @see IntValueSelectorMin
 * @see IntValueSelectorMax
 */
public class IntValueSelectorMinMax implements IntValueSelector {
    private boolean _min;

    /**
     * Default constructor initializes first selection of the minimum value from the domain of the variable
     */
    public IntValueSelectorMinMax() {
        _min = true;
    }

    /**
     * Returns the minimal or maximal value from the domain of the variable.
     */
    public int select(IntVar intvar) {
        int value;
        if (_min) {
            _min = false;
            value = intvar.min();
        } else {
            _min = true;
            value = intvar.max();
        }
        return value;
    }

} // ~IntValueSelectorMinMax
