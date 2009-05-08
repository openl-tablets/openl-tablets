package com.exigen.ie.constrainer;

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
    static public int MIN_VALUE = -Integer.MAX_VALUE;

    /**
     * The largest value of type int.
     */
    static public int MAX_VALUE = Integer.MAX_VALUE;

    /**
     * Returns the expression: <code>abs(this)</code>.
     */
    public IntExp abs();

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
    public IntExp add(int value);

    /**
     * Returns the expression: <code>(this + exp)</code>.
     */
    public IntExp add(IntExp exp);

    /**
     * Returns the cast of this integer expression into floating-point
     * expression.
     *
     * @return the FloatExp equals to this.
     */
    public FloatExp asFloat();

    public IntExp bitAnd(IntExp exp);

    /**
     * Returns true if this expression is bound. An expression is considered
     * bound if contains only one value in its domain.
     *
     * @return <code>true</code> if this expression is bound: it's domain has
     *         only one value.
     */
    public boolean bound();

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
    public boolean contains(int value);

    /**
     * Returns the expression: <code>(this / value)</code>.
     */
    public FloatExp div(double value);

    /**
     * Returns the expression: <code>(this / value)</code>.
     */
    public IntExp div(int value);

    /**
     * Returns the expression: <code>(this / divisor)</code>.
     */
    public IntExp div(IntExp divisor) throws Failure;

    /**
     * Returns the display string for the current domain of this expression.
     */
    public String domainToString();

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    public IntBoolExp eq(double value);

    /**
     * Returns the boolean expression: <code>(this == value)</code>.
     */
    public IntBoolExp eq(int value);

    /**
     * Returns the boolean expression: <code>(this == exp)</code>.
     */
    public IntBoolExp eq(IntExp exp);

    /**
     * Returns the constraint: <code>(this == exp + value)</code>.
     */
    public Constraint equals(FloatExp exp, double value);

    /**
     * Returns the constraint: <code>(this == value)</code>.
     */
    public Constraint equals(int value);

    /**
     * Returns the constraint: <code>(this == exp)</code>.
     */
    public Constraint equals(IntExp exp);

    /**
     * Returns the constraint: <code>(this == exp)</code>.
     */
    // public Constraint equals(FloatExp exp);
    /**
     * Returns the constraint: <code>(this == exp + value)</code>.
     */
    public Constraint equals(IntExp exp, int value);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    public IntBoolExp ge(double value);

    /**
     * Returns the boolean expression: <code>(this >= value)</code>.
     */
    public IntBoolExp ge(int value);

    /**
     * Returns the boolean expression: <code>(this >= exp)</code>.
     */
    public IntBoolExp ge(IntExp exp);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    public IntBoolExp gt(double value);

    /**
     * Returns the boolean expression: <code>(this > value)</code>.
     */
    public IntBoolExp gt(int value);

    /**
     * Returns the boolean expression: <code>(this > exp)</code>.
     */
    public IntBoolExp gt(IntExp exp);

    /**
     * Iterates the domain of this expression by calling
     * <code>doSomethingOrStop(int val)</code> of
     * <code>IntDomainIterator</code>.
     *
     * @see IntDomainIterator
     * @see IntDomainIterator#doSomethingOrStop
     */
    public void iterateDomain(IntDomainIterator it) throws Failure;

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    public IntBoolExp le(double value);

    /**
     * Returns the boolean expression: <code>(this <= value)</code>.
     */
    public IntBoolExp le(int value);

    /**
     * Returns the boolean expression: <code>(this <= exp)</code>.
     */
    public IntBoolExp le(IntExp exp);

    /**
     * Returns the constraint: <code>(this < value)</code>.
     */
    public Constraint less(int value);

    /**
     * Returns the constraint: <code>(this < exp)</code>.
     */
    public Constraint less(IntExp exp);

    /**
     * Returns the constraint: <code>(this <= exp)</code>.
     */
    public Constraint lessOrEqual(FloatExp exp);

    /**
     * Returns the constraint: <code>(this <= value)</code>.
     */
    public Constraint lessOrEqual(int value);

    /**
     * Returns the constraint: <code>(this <= exp)</code>.
     */
    public Constraint lessOrEqual(IntExp exp);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    public IntBoolExp lt(double value);

    /**
     * Returns the boolean expression: <code>(this < value)</code>.
     */
    public IntBoolExp lt(int value);

    /**
     * Returns the boolean expression: <code>(this < exp)</code>.
     */
    public IntBoolExp lt(IntExp exp);

    /**
     * Returns the largest value of the domain of this expression.
     */
    public int max();

    /**
     * Returns the smallest value of the domain of this expression.
     */
    public int min();

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    public FloatExp mod(double value);

    /**
     * Returns the expression: <code>(this % value)</code>.
     */
    public IntExp mod(int value);

    /**
     * Returns the constraint: <code>(this > value)</code>.
     */
    public Constraint more(int value);

    /**
     * Returns the constraint: <code>(this > exp)</code>.
     */
    public Constraint more(IntExp exp);

    /**
     * Returns the constraint: <code>(this >= exp)</code>.
     */
    public Constraint moreOrEqual(FloatExp exp);

    /**
     * Returns the constraint: <code>(this >= value)</code>.
     */
    public Constraint moreOrEqual(int value);

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
    public IntExp mul(int value);

    /**
     * Returns the expression: <code>(this * exp)</code>.
     */
    public IntExp mul(IntExp exp);

    /**
     * Returns the boolean expression: <code>(this != value)</code>.
     */
    public IntBoolExp ne(double value);

    /**
     * Returns the boolean expression: <code>(this != value)</code>.
     */
    public IntBoolExp ne(int value);

    /**
     * Returns the boolean expression: <code>(this != exp)</code>.
     */
    public IntBoolExp ne(IntExp exp);

    /**
     * Returns the expression: <code>(-this)</code>. That expression is
     * opposite by sign to this expression.
     */
    public IntExp neg();

    /**
     * Returns the expression: <code>pow(this,value)</code>.
     */
    public FloatExp pow(double value) throws Failure;

    /**
     * Returns the expression: <code>pow(this,value)</code>. Value should be >=
     * 0. For value < 0 use pow(double value).
     *
     * Throws RuntimeException if there value < 0.
     */
    public IntExp pow(int value);

    /**
     * Returns the expression: <code>pow(this,pow_exp)</code>. The constraint
     * "pow_exp >= 0" is added to Costrainer.
     *
     * Throws RuntimeException if there value < 0.
     */
    public IntExp pow(IntExp pow_exp) throws Failure;

    /**
     * Returns the expression:
     * <code>((this < rangeMin)*(rangeMin - this) + (this > rangeMax)*(this - rangeMax))</code>.
     */
    public IntExp rangeViolation(int rangeMin, int rangeMax);

    /**
     * Removes the range [min..max] from the domain of this expression.
     *
     * @param min range minimum value.
     * @param max range maxinum value.
     * @throws Failure if domain becomes empty.
     */
    public void removeRange(int min, int max) throws Failure;

    /**
     * Removes the value from the domain of this expression.
     *
     * @throws Failure if domain becomes empty.
     */
    public void removeValue(int value) throws Failure;

    /**
     * Sets the maximum value for this expression.
     *
     * @param max new maximum value.
     * @throws Failure if domain becomes empty.
     */
    public void setMax(int max) throws Failure;

    /**
     * Sets the minimum value for this expression.
     *
     * @param min new miminum value.
     * @throws Failure if domain becomes empty.
     */
    public void setMin(int min) throws Failure;

    /**
     * Bounds this variable to be equal to the value.
     *
     * @param value a value to be set.
     * @throws Failure when: <code>!this.contains(value)</code>.
     * @see #bound
     */
    public void setValue(int value) throws Failure;

    /**
     * Returns the size (cardinality) of the domain of this expression. The
     * domain's size is the number of integer values the domain.
     *
     * @return the number of values in the domain of this variable.
     */
    public int size();

    /**
     * Returns the expression: <code>(this * this)</code>.
     */
    public IntExp sqr();

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
    public IntExp sub(int value);

    /**
     * Returns the expression: <code>(this - exp)</code>.
     */
    public IntExp sub(IntExp exp);

    /**
     * Returns true if domain is not empty.
     *
     * @return true if domain is not empty.
     */
    public boolean valid();

    /**
     * Returns the value this expression if it is bound.
     *
     * @return the value of this expresion.
     * @throws Failure if this expresionis not bound.
     *
     * @see #bound
     */
    public int value() throws Failure;

    /**
     *
     * @return value without checking bound(); should be called only after the
     *         caller checked the bounds already
     */
    public int valueUnsafe();

} // ~IntExp
