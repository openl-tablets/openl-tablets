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
 * An interface for constrainer object. Constrained variables, expressions, constraints, and goals implement that
 * interface.
 *
 * @see IntExp
 * @see FloatExp
 * @see Constraint
 * @see Goal
 * @see Constrainer
 */
public interface ConstrainerObject extends java.io.Serializable {
    /**
     * Returns the constrainer the object belongs to. Constrainer object belongs to one and only one constrainer.
     *
     * @return the constrainer the object belongs to.
     */
    Constrainer constrainer();

    /**
     * Returns the name of the object. If the name is undefined the function returns an empty string.
     *
     * @return The name of the object.
     */
    String name();

    /**
     * Sets the name of the object.
     *
     * @param name The name to be set.
     */
    void name(String name);

    /**
     * Returns the object associated with this constrainer object. It is possible to associate some object with a
     * constrainer object. This method returns the associated object if it exists. Otherwise the method returns null.
     *
     * @return the object associated with the constrainer object.
     */
    Object object();

    /**
     * Associates the object with this constrainer object. It is possible to associate an object with an constrainer
     * object. When your business object BO is assosiated with the constraned object CO (i.e. IntVar), it is convenient
     * to use this method to save the reference to the BO in the CO.
     */
    void object(Object o);

}
