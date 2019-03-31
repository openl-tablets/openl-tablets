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
 * An implementation of the IntValueSelector interface that selects the maximal value from the domain of the variable.
 *
 * @see GoalInstantiate
 * @see GoalGenerate
 * @see IntValueSelectorMin
 * @see IntValueSelectorMinMax
 */
public class IntValueSelectorMax implements IntValueSelector {
    /**
     * Return the maximal value from the domain of the variable.
     */
    @Override
    public int select(IntVar intvar) {
        return intvar.max();
    }

} // ~ IntValueSelectorMax
