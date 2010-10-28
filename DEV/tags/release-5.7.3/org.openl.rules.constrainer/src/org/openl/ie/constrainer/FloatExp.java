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
 * An interface for the constrained floating-point expression. Any arithmetic
 * operation where one of the operands is the floating-point variable/expression
 * returns the floating-point expression.
 */
public interface FloatExp extends Expression {
    /**
     * Returns the expression: <code>abs(this)</code>.
     */
    public FloatExp abs();

    /**
     * Returns the expression: <code>(this + value)</code>.
     */
    public FloatExp add(double value);

    /**
     * Returns the expression: <code>(this + exp)</code>.
     */
    public FloatExp add(FloatExp exp);

    /**
     * Returns the expression: <code>(this + value)</code>.
     */
    public FloatExp add(int value);

    /**
     * Returns the expression: <code>(this + exp)</code>.
     */
    public FloatExp add(IntExp exp);

    /**
     * Returns truie if this expression is bound.
     *
     * The floating-point expression is bound when
     *
     * <pre>
     * (max-min)/max(1,|min|) &lt;= precision
     * </pre>
     *
     * The floating-point expression is bound to the <b>mean value</b> in the
     * <code>[min..max]</code> interval associated with this expresion.
     *
     * @return true if this expression is bound.
     */
    public boolean bound();

    /**
     * Returns the expression: <code>(this / value)</code>.
     */
    public FloatExp div(double value);

    /**
     * Returns the expression: <code>(this / exp)</code>.
     */
    public FloatExp div(FloatExp exp);

    /**
     * Returns the expression: <code>(this / value)</code>.
     */
    public FloatExp div(int value);

    /**
     * Returns the expression: <code>(this / exp)</code>.
     */
    public FloatExp div(IntExp exp);

    /**
     * Returns the display string for the current domain of this expression.
     */
    public String domainToString();

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    public IntBoolExp eq(double value);

    /**
     * Returns the boolean expression: <code>(this == exp)</code>.
     */
    public IntBoolExp eq(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    public IntBoolExp eq(int value);

    /**
     * Returns the constraint: <code>(this == value)</code>.
     */
    public Constraint equals(double value);

    /**
     * Returns the constraint: <code>(this == exp)</code>.
     */
    public Constraint equals(FloatExp exp);

    /**
     * Returns the constraint: <code>(this == exp + value)</code>.
     */
    public Constraint equals(FloatExp exp, double value);

    /**
     * Returns the constraint: <code>(this == exp)</code>.
     */
    public Constraint equals(IntExp exp);

    /**
     * Returns the constraint: <code>(this == exp + value)</code>.
     */
    public Constraint equals(IntExp exp, double value);

    /**
     * Returns the expression: <code>exp(this)</code>.
     */
    public FloatExp exp();

    /**
     * Returns the expression: <code>pow(value,this)</code>.
     */
    public FloatExp exp(double value);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    public IntBoolExp ge(double value);

    /**
     * Returns the boolean expression: <code>(this >= exp)</code>.
     */
    public IntBoolExp ge(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    public IntBoolExp ge(int value);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    public IntBoolExp gt(double value);

    /**
     * Returns the boolean expression: <code>(this > exp)</code>.
     */
    public IntBoolExp gt(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    public IntBoolExp gt(int value);

    /**
     * Returns the expression: <code>(1/this)</code>.
     */
    public FloatExp inv();

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    public IntBoolExp le(double value);

    /**
     * Returns the boolean expression: <code>(this <= exp)</code>.
     */
    public IntBoolExp le(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    public IntBoolExp le(int value);

    /**
     * Returns the constraint: <code>(this <= value)</code>.
     */
    public Constraint lessOrEqual(double value);

    /**
     * Returns the constraint: <code>(this <= exp)</code>.
     */
    public Constraint lessOrEqual(FloatExp exp);

    /**
     * Returns the constraint: <code>(this <= exp)</code>.
     */
    public Constraint lessOrEqual(IntExp exp);

    /**
     * Returns the expression: <code>log(this)</code>.
     */
    public FloatExp log() throws Failure;

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    public IntBoolExp lt(double value);

    /**
     * Returns the boolean expression: <code>(this < exp)</code>.
     */
    public IntBoolExp lt(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    public IntBoolExp lt(int value);

    /**
     * Returns the largest value of the domain of this expression.
     */
    public double max();

    /**
     * Returns the smallest value of the domain of this expression.
     */
    public double min();

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    public FloatExp mod(double value);

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    public FloatExp mod(int value);

    /**
     * Returns the constraint: <code>(this >= value)</code>.
     */
    public Constraint moreOrEqual(double value);

    /**
     * Returns the constraint: <code>(this >= exp)</code>.
     */
    public Constraint moreOrEqual(FloatExp exp);

    /**
     * Returns the constraint: <code>(this >= exp)</code>.
     */
    public Constraint moreOrEqual(IntExp exp);

    /**
     * Returns the expression: <code>(this * value)</code>.
     */
    public FloatExp mul(double value);

    /**
     * Returns the expression: <code>(this * exp)</code>.
     */
    public FloatExp mul(FloatExp exp);

    /**
     * Returns the expression: <code>(this * value)</code>.
     */
    public FloatExp mul(int value);

    /**
     * Returns the expression: <code>(this * exp)</code>.
     */
    public FloatExp mul(IntExp exp);

    /**
     * Returns the boolean expression: <code>(this != value)</code>.
     */
    public IntBoolExp ne(double value);

    /**
     * Returns the boolean expression: <code>(this != exp)</code>.
     */
    public IntBoolExp ne(FloatExp exp);

    /**
     * Returns the boolean expression: <code>(this != value)</code>.
     */
    public IntBoolExp ne(int value);

    /**
     * Returns the expression: <code>(-this)</code>. That expression is
     * opposite by sign to this expression.
     */
    public FloatExp neg();

    /**
     * Returns the expression: <code>pow(this,value)</code>.
     */
    public FloatExp pow(double value) throws Failure;

    /**
     * Returns the expression: <code>pow(this,exp)</code>.
     *
     * Throws RuntimeException if there exists invalid values for the
     * expressions in domain.
     */
    public FloatExp pow(FloatExp exp) throws Failure;

    /**
     * Returns the expression: <code>pow(this,value)</code>.
     *
     * Throws RuntimeException if there exists invalid values for the
     * expressions in domain.
     */
    public FloatExp pow(int value) throws Failure;

    /**
     * Returns the expression: <code>pow(this,exp)</code>.
     *
     * Throws RuntimeException if there exists invalid values for the
     * expressions in domain.
     */
    public FloatExp pow(IntExp exp) throws Failure;

    /**
     * Remove the (min..max) range from the domain of this expression.
     *
     * @param min range minimum value.
     * @param max range maxinum value.
     * @throws Failure if domain becomes empty.
     */
    public void removeRange(double min, double max) throws Failure;

    /**
     * Sets the maximum value for this expression.
     *
     * @param max new maximum value.
     * @throws Failure if domain becomes empty.
     */
    public void setMax(double max) throws Failure;

    /**
     * Sets the minimum value for this expression.
     *
     * @param min new minimum value.
     * @throws Failure if domain becomes empty.
     */
    public void setMin(double min) throws Failure;

    /**
     * Bounds this expression to be equal to the value.
     *
     * @see #bound
     */
    public void setValue(double value) throws Failure;

    /**
     * Returns the size of the domain for this expression:
     * <code>(max - min)</code>.
     *
     * @return <code>(max - min)</code>.
     */
    public double size();

    /**
     * Returns the expression: <code>(this * this)</code>.
     */
    public FloatExp sqr();

    /**
     * Returns the expression: <code>(this - value)</code>.
     */
    public FloatExp sub(double value);

    /**
     * Returns the expression: <code>(this - exp)</code>.
     */
    public FloatExp sub(FloatExp exp);

    /**
     * Returns the expression: <code>(this - value)</code>.
     */
    public FloatExp sub(int value);

    /**
     * Returns the expression: <code>(this - exp)</code>.
     */
    public FloatExp sub(IntExp exp);

    /**
     * Returns the value this expression if it is bound.
     *
     * The floating-point expression is bound to the <b>mean value</b> in the
     * <code>[min..max]</code> interval associated with this expresion.
     *
     * @return the value of this bound expression.
     *
     * @throws Failure if the expression is not bound.
     */
    public double value() throws Failure;

} // ~FloatExp
