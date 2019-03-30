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
import java.util.Enumeration;
import java.util.Vector;

/**
 * An abstract implementation of the factory for reusable objects.
 *
 * Any concrete factory should implement the method: createNewElement(). This method returns a new uninitialized object
 * for the conctete factory.
 *
 * @see Reusable
 */
// "implements serializable" was added by Eugeny Tseitlin 18.06.2003
public abstract class ReusableFactory implements java.io.Serializable {
    /**
     * All created factories.
     */
    protected static Vector _allFactories = new Vector();
    protected FastStack _reusables;

    protected int _element_counter;

    /**
     * Performs cleanup for all factories.
     */
    public static void cleanAll() {
        Enumeration it = _allFactories.elements();

        while (it.hasMoreElements()) {
            ((ReusableFactory) it.nextElement()).cleanUp();
        }
    }

    /**
     * Prints the statistics for all factories.
     */
    public static synchronized void printStatistics(java.io.PrintStream s) {
        s.println("Class,Count");
        for (int i = 0; i < _allFactories.size(); i++) {
            ReusableFactory factory = (ReusableFactory) _allFactories.elementAt(i);
            s.println(factory.getClass() + "," + factory.getElementCount());
        }
    }

    /**
     * Default constructor initializes this factory and registeres this factory in all factories.
     */
    public ReusableFactory() {
        _reusables = new FastStack();
        _element_counter = 0;
        register();
    }

    /**
     * Performs cleanup for this factory.
     */
    public void cleanUp() {
        _reusables = new FastStack();
        _element_counter = 0;
    }

    /**
     * Creates new uninitialized object for this factory.
     */
    abstract protected Reusable createNewElement();

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
        _element_counter++;
        if (_reusables.empty()) {
            result = createNewElement();
            result.setFactory(this);
        } else {
            result = (Reusable) _reusables.pop();
        }
        return result;
    }

    /**
     * Returns the number of elements returned by this factory.
     */
    public int getElementCount() {
        return _element_counter;
    }

    /**
     * Registers this factory in all factories.
     */
    void register() {
        _allFactories.addElement(this);
    }

} // ~ReusableFactory
