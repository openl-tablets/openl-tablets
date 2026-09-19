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

import java.io.Serializable;

/**
 * An abstract implementation of the factory for reusable objects.
 * <p>
 * Any concrete factory should implement the method: createNewElement(). This method returns a new uninitialized object
 * for the conctete factory.
 *
 * @see Reusable
 */
// "implements serializable" was added by Eugeny Tseitlin 18.06.2003
public abstract class ReusableFactory implements Serializable {
    protected FastStack _reusables;

    /**
     * Default constructor initializes this factory.
     */
    public ReusableFactory() {
        _reusables = new FastStack();
    }

    /**
     * Creates new uninitialized object for this factory.
     */
    protected abstract Reusable createNewElement();

    /**
     * Returns the unused object to the factory.
     */
    public final synchronized void freeElement(Reusable element) {
        _reusables.push(element);
    }

    /**
     * Returns next the uninitialized object for this factory. If there are unused objects one of them is returned.
     * Otherwise new object is created and returned.
     */
    public final synchronized Object getElement() {
        Reusable result;
        if (_reusables.empty()) {
            result = createNewElement();
            result.setFactory(this);
        } else {
            result = (Reusable) _reusables.pop();
        }
        return result;
    }

} // ~ReusableFactory
