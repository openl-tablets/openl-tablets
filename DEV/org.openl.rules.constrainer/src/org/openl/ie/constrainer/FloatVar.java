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
//
//: FloatVar.java
//
/**
 * An interface for the constrained floating-point variable. The following code creates a floating-point variable:
 *
 * <pre>
 * FloatVar var = constrainer.addFloatVar(min, max, name);
 * </pre>
 */
public interface FloatVar extends FloatExp {
    /**
     * Undo helper: sets the maximum value for the domain of this variable.
     */
    void forceMax(double max);

    /**
     * Undo helper: sets the minimum value for the domain of this variable.
     */
    void forceMin(double min);

    /**
     * Returns a goal that instantiates this variable.
     *
     * @return a goal that instantiates this variable.
     */
    Goal instantiate();

} // ~FloatVar
