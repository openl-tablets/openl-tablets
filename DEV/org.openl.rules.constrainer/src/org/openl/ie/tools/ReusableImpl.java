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
 * A generic implementation for Reusable interface.
 *
 * The classes that implement Reusable interface usually extends ReusableImpl.
 *
 * @see Reusable
 * @see ReusableFactory
 */
public class ReusableImpl implements Reusable {
    protected ReusableFactory _factory;

    /**
     * Default constructor.
     */
    public ReusableImpl() {
    }

    // public final void free()
    @Override
    public void free() {
        // if (_factory != null)
        _factory.freeElement(this);
    }

    @Override
    public final ReusableFactory getFactory() {
        return _factory;
    }

    @Override
    public final void setFactory(ReusableFactory factory) {
        _factory = factory;
    }

} // ~ReusableImpl
