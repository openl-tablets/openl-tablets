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
//: FloatExp.java
//
/**
 * An interface for the constrained floating-point expression. Any arithmetic operation where one of the operands is the
 * floating-point variable/expression returns the floating-point expression.
 */
public interface FloatExp extends Expression {

    /**
     * Returns the expression: <code>(this + value)</code>.
     */
    FloatExp add(double value);

    /**
     * Returns truie if this expression is bound.
     *
     * The floating-point expression is bound when
     *
     * <pre>
     * (max-min)/max(1,|min|) &lt;= precision
     * </pre>
     *
     * The floating-point expression is bound to the <b>mean value</b> in the <code>[min..max]</code> interval
     * associated with this expresion.
     *
     * @return true if this expression is bound.
     */
    boolean bound();

    /**
     * Returns the display string for the current domain of this expression.
     */
    String domainToString();

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    IntBoolExp eq(double value);

    /**
     * Returns the boolean expression: <code>(this == exp)</code>.
     */
    IntBoolExp eq(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    IntBoolExp eq(int value);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    IntBoolExp ge(double value);

    /**
     * Returns the boolean expression: <code>(this >= exp)</code>.
     */
    IntBoolExp ge(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    IntBoolExp ge(int value);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    IntBoolExp gt(double value);

    /**
     * Returns the boolean expression: <code>(this > exp)</code>.
     */
    IntBoolExp gt(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    IntBoolExp gt(int value);

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    IntBoolExp le(double value);

    /**
     * Returns the boolean expression: <code>(this <= exp)</code>.
     */
    IntBoolExp le(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    IntBoolExp le(int value);

    /**
     * Returns the constraint: <code>(this <= value)</code>.
     */
    Constraint lessOrEqual(double value);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    IntBoolExp lt(double value);

    /**
     * Returns the boolean expression: <code>(this < exp)</code>.
     */
    IntBoolExp lt(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    IntBoolExp lt(int value);

    /**
     * Returns the largest value of the domain of this expression.
     */
    double max();

    /**
     * Returns the smallest value of the domain of this expression.
     */
    double min();

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    FloatExp mod(double value);

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    FloatExp mod(int value);

    /**
     * Returns the constraint: <code>(this >= value)</code>.
     */
    Constraint moreOrEqual(double value);

    /**
     * Remove the (min..max) range from the domain of this expression.
     *
     * @param min range minimum value.
     * @param max range maxinum value.
     * @throws Failure if domain becomes empty.
     */
    void removeRange(double min, double max) throws Failure;

    /**
     * Sets the maximum value for this expression.
     *
     * @param max new maximum value.
     * @throws Failure if domain becomes empty.
     */
    void setMax(double max) throws Failure;

    /**
     * Sets the minimum value for this expression.
     *
     * @param min new minimum value.
     * @throws Failure if domain becomes empty.
     */
    void setMin(double min) throws Failure;

    /**
     * Bounds this expression to be equal to the value.
     *
     * @see #bound
     */
    void setValue(double value) throws Failure;

    /**
     * Returns the size of the domain for this expression: <code>(max - min)</code>.
     *
     * @return <code>(max - min)</code>.
     */
    double size();

    /**
     * Returns the value this expression if it is bound.
     *
     * The floating-point expression is bound to the <b>mean value</b> in the <code>[min..max]</code> interval
     * associated with this expresion.
     *
     * @return the value of this bound expression.
     *
     * @throws Failure if the expression is not bound.
     */
    double value() throws Failure;

} // ~FloatExp
