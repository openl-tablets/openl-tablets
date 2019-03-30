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
 * An interface for the selection of the values from the domain of the IntVar. Used in GoalInstantiate.
 *
 * @see GoalInstantiate
 * @see GoalGenerate
 * @see IntValueSelectorMin
 * @see IntValueSelectorMax
 * @see IntValueSelectorMinMax
 */
public interface IntValueSelector extends java.io.Serializable {
    /**
     * The type of the selector: selects the minimal value.
     */
    public static final int MIN = 0;

    /**
     * The type of the selector: selects the maximal value.
     */
    public static final int MAX = 1;

    /**
     * Returns the selected value from the domain of the IntVar.
     */
    public int select(IntVar var);

}
