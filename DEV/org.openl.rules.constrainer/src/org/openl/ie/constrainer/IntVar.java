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
//: IntVar.java
//
/**
 * An interface for the constrained integer variable. The following code creates an integer variable:
 *
 * <pre>
 * IntVar var = constrainer.addIntVar(min, max, name);
 * </pre>
 */
public interface IntVar extends IntExp {

    /**
     * The type of the domain implementation: plain. This implementation keeps only min/max values.
     */
    public static final int DOMAIN_PLAIN = 0;

    /**
     * The type of the domain implementation: fast.
     */
    public static final int DOMAIN_BIT_FAST = 1;

    /**
     * The type of the domain implementation: small.
     */
    public static final int DOMAIN_BIT_SMALL = 2;

    /**
     * The type of the domain implementation: boolean [0..1].
     */
    public static final int DOMAIN_BOOL = 3;

    /**
     * The type of the domain implementation: default. The concrete type is choosen depending on the state of the
     * domain.
     */
    public static final int DOMAIN_DEFAULT = -1;

    /**
     * Returns the domain type of this variable.
     *
     * @return the domain type of this variable.
     */
    public int domainType();

    /**
     * Undo helper: insert the value into domain of this variable.
     */
    public void forceInsert(int val);

    /**
     * Undo helper: sets the maximum value for the domain of this variable.
     */
    public void forceMax(int val);

    /**
     * Undo helper: sets the minimum value for the domain of this variable.
     */
    public void forceMin(int val);

    /**
     * Undo helper: sets the size for the domain of this variable.
     */
    public void forceSize(int val);

    /**
     * Returns a goal that instantiates this variable.
     *
     * @return a goal that instantiates this variable.
     */
    public Goal instantiate();

} // ~IntVar
