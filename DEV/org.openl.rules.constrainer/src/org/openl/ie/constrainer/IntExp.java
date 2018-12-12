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
//: IntExp.java
//
/**
 * An interface for the constrained integer expression. Any arithmetic operation
 * where the operands are the integer variables/expressions returns the integer
 * expression.
 */
public interface IntExp extends Expression {

    /**
     * An interface used in the iteration of the domain of the integer
     * expression.
     *
     * @see #iterateDomain
     */
    public interface IntDomainIterator {
        /**
         * Processes each value contained within domain one by one (by
         * convention from lowest to highest value).
         *
         * It should return <code>false</code> to stop iteration or
         * <code>true</code> to continue.
         *
         * @see IntExp#iterateDomain
         */
        public boolean doSomethingOrStop(int val) throws Failure;
    }

    /**
     * The smallest value of type int. Please notice that
     * <code>MIN_VALUE == -MAX_VALUE</code> which is not true for
     * <code>java.lang.Integer</code>.
     */
    int MIN_VALUE = -Integer.MAX_VALUE;

    /**
     * The largest value of type int.
     */
    int MAX_VALUE = Integer.MAX_VALUE;

    /**
     * Returns the expression: <code>(this + value)</code>.
     */
    IntExp add(int value);

    /**
     * Returns the expression: <code>(this + exp)</code>.
     */
    IntExp add(IntExp exp);

    /**
     * Returns true if this expression is bound. An expression is considered
     * bound if contains only one value in its domain.
     *
     * @return <code>true</code> if this expression is bound: it's domain has
     *         only one value.
     */
    boolean bound();

    /**
     * Returns true if the domain of this expression contains the value.
     *
     * <code>NOTE: in some cases it is not possible (or feasible) to calculate <code>contains</code> with
     * 100% accuracy. We also can not guarantee a consistency between <code>contains</code> and
     * <code>size</code> method for all expressions. In other words if <code>contains(value)</code> returns
     * <code>true</code> it might happen that value does not belong to the expression's domain.
     * The opposite (<code>false</code>) is always true :).
     *
     * @return <code>true</code> if the expression contains the value in it's domain.
     */
    boolean contains(int value);

    /**
     * Returns the display string for the current domain of this expression.
     */
    String domainToString();

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    IntBoolExp eq(int value);

    /**
     * Returns the constraint: <code>(this == value)</code>.
     */
    Constraint equals(int value);

    /**
     * Returns the constraint: <code>(this == exp)</code>.
     */
    Constraint equals(IntExp exp);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    IntBoolExp ge(int value);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    IntBoolExp gt(int value);

    /**
     * Returns the boolean expression: <code>(this > exp)</code>.
     */
    IntBoolExp gt(IntExp exp);

    /**
     * Iterates the domain of this expression by calling
     * <code>doSomethingOrStop(int val)</code> of
     * <code>IntDomainIterator</code>.
     *
     * @see IntDomainIterator
     * @see IntDomainIterator#doSomethingOrStop
     */
    void iterateDomain(IntDomainIterator it) throws Failure;

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    IntBoolExp le(int value);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    IntBoolExp lt(int value);

    /**
     * Returns the boolean expression: <code>(this < exp)</code>.
     */
    IntBoolExp lt(IntExp exp);

    /**
     * Returns the largest value of the domain of this expression.
     */
    int max();

    /**
     * Returns the smallest value of the domain of this expression.
     */
    int min();

    /**
     * Removes the range [min..max] from the domain of this expression.
     *
     * @param min range minimum value.
     * @param max range maxinum value.
     * @throws Failure if domain becomes empty.
     */
    void removeRange(int min, int max) throws Failure;

    /**
     * Removes the value from the domain of this expression.
     *
     * @throws Failure if domain becomes empty.
     */
    void removeValue(int value) throws Failure;

    /**
     * Sets the maximum value for this expression.
     *
     * @param max new maximum value.
     * @throws Failure if domain becomes empty.
     */
    void setMax(int max) throws Failure;

    /**
     * Sets the minimum value for this expression.
     *
     * @param min new miminum value.
     * @throws Failure if domain becomes empty.
     */
    void setMin(int min) throws Failure;

    /**
     * Bounds this variable to be equal to the value.
     *
     * @param value a value to be set.
     * @throws Failure when: <code>!this.contains(value)</code>.
     * @see #bound
     */
    void setValue(int value) throws Failure;

    /**
     * Returns the size (cardinality) of the domain of this expression. The
     * domain's size is the number of integer values the domain.
     *
     * @return the number of values in the domain of this variable.
     */
    int size();

    /**
     * Returns the value this expression if it is bound.
     *
     * @return the value of this expresion.
     * @throws Failure if this expresionis not bound.
     *
     * @see #bound
     */
    int value() throws Failure;

    /**
     *
     * @return value without checking bound(); should be called only after the
     *         caller checked the bounds already
     */
    int valueUnsafe();

} // ~IntExp
