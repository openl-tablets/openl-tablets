package org.openl.ie.constrainer;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of FloatelEngine, Inc., USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////
/**
 * An interface for the selection of the integer variable from a given array of variables. Used in GoalFloatGenerate.
 *
 * @see GoalFloatGenerate
 * @see FloatVarSelectorFirstUnbound
 * @see FloatVarSelectorMinSize
 * @see FloatVarSelectorMinSizeMin
 */
public interface FloatVarSelector {
    /**
     * Returns the index of the selected variable in the array of FloatVar(s). If no variables to select, it returns -1;
     */
    public int select();
}
