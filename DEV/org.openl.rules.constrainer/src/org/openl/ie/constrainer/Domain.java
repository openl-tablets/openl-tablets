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
//import java.util.Set;
/**
 * An interface for the domain of the constrained integer variable. Domain is a set of the possible values of the
 * constrained variables.
 *
 * @see IntVar
 */
public interface Domain extends java.io.Serializable {
    /**
     * Returns a constrainer that owns this domain.
     */
    Constrainer constrainer();

    /**
     * Returns true if this domain contains the value.
     */
    boolean contains(int value);

    /**
     * Undo helper: insert the value into this domain.
     */
    void forceInsert(int val);

    /**
     * Undo helper: sets the maximum value for this domain.
     */
    void forceMax(int max);

    /**
     * Undo helper: sets the minimum value for this domain.
     */
    void forceMin(int min);

    /**
     * Undo helper: sets the size for this domain.
     */
    void forceSize(int val);

    /**
     * Iterates this domain calling it.doSomethingOrStop() for each values in the domain.
     */
    void iterateDomain(IntExp.IntDomainIterator it) throws Failure;

    /**
     * Returns the largest value in this domain.
     */
    int max();

    /**
     * Returns the smallest value in this domain.
     */
    int min();

    /**
     * Removes the range from this domain.
     *
     * @throws Failure if domain becomes empty.
     */
    boolean removeRange(int min, int max) throws Failure;

    /**
     * Removes the value from this domain.
     *
     * @throws Failure if domain becomes empty.
     */
    boolean removeValue(int value) throws Failure;

    /**
     * Sets the maximum value for this domain.
     *
     * @throws Failure if domain becomes empty.
     */
    boolean setMax(int M) throws Failure;

    /**
     * Sets the minimum value for this domain.
     *
     * @throws Failure if domain becomes empty.
     */
    boolean setMin(int m) throws Failure;

    /**
     * Sets this domain to the damain containing only the value.
     *
     * @throws Failure if domain becomes empty.
     */
    boolean setValue(int value) throws Failure;

    /**
     * Returns the size (cardinality) of this domain.
     */
    int size();

    /**
     * Returns the type of this domain.
     */
    int type();

    /**
     * Sets the variable associated with this domain.
     */
    void variable(IntVar var);

    // public Set set();

} // ~Domain
