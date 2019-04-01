package org.openl.ie.tools;

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
 * An interface for the reusable object. Reusable objects allow the developer to reuse the memory allocated and freed
 * for the objects of the same class.
 *
 * Any reusable object belongs to the single ReusableFactory. The factory creates the objects and the objects are
 * returned to the factory when they are no longer used.
 *
 * @see ReusableFactory
 */
// "implements serializable" was added by Eugeny Tseitlin 18.06.2003
public interface Reusable extends java.io.Serializable {
    /**
     * Clean-up the object and returns it to the factory that owns this object.
     */
    void free();

    /**
     * Returns the factory that owns this object.
     */
    ReusableFactory getFactory();

    /**
     * Sets the factory that owns this object.
     */
    void setFactory(ReusableFactory factory);

} // ~Reusable
