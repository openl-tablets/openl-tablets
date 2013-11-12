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
//: Expression.java
//
/**
 * An interface for the constrained integer and floating-point expressions.
 */
import java.util.Map;

public interface Expression extends Subject, EventOfInterest.Constants, java.io.Serializable {
    /**
     * The MIN tracing mask for the traced variables.
     *
     * @see Constrainer#addIntVarTrace
     * @see Constrainer#addFloatVarTrace
     */
    public static int TRACE_MIN = 1;

    /**
     * The MAX tracing mask for the traced variables.
     *
     * @see Constrainer#addIntVarTrace
     * @see Constrainer#addFloatVarTrace
     */
    public static int TRACE_MAX = 2;

    /**
     * The REMOVE tracing mask for the traced variables.
     *
     * @see Constrainer#addIntVarTrace
     */
    public static int TRACE_REMOVE = 4;

    /**
     * The VALUE tracing mask for the traced variables.
     *
     * @see Constrainer#addIntVarTrace
     * @see Constrainer#addFloatVarTrace
     */
    public static int TRACE_VALUE = 8;

    // /**
    // * The HISTORY tracing mask for the traced variables.
    // * @see Constrainer#addIntVarTrace
    // */
    // public static int TRACE_HISTORY = 16;

    /**
     * The ALL tracing mask for the traced variables.
     *
     * @see Constrainer#addIntVarTrace
     * @see Constrainer#addFloatVarTrace
     */
    public static int TRACE_ALL = 0xffffffff;

    /**
     * If the expression is linear one the function will fill out the Map with
     * pairs of variables and their coefficients, otherwise it throws an
     * exception.
     *
     * @param map The map to be filled out with the pairs (var, var's
     *            coefficient);
     * @return Free term of an expression
     * @throws NonLinearExpression if the expression is not linear
     */
    public double calcCoeffs(Map map) throws NonLinearExpression;

    /**
     * It works the same way as {@link #calcCoeffs(Map)} does but in addition to
     * that it multiplies all the coefficients by it's second argument. Invoked
     * with the unity as the second argument it produces the same result as
     * {@link #calcCoeffs(Map)}
     *
     * @param map The map to be filled out with the pairs (var, var's
     *            coefficient);
     * @param factor Additional factor for the whole expression
     * @return Free term of an expression
     */
    public double calcCoeffs(Map map, double factor) throws NonLinearExpression;

    /**
     * Checks wether the expression is linear or not.
     *
     * @return true if the expression is linear, false otherwise
     */
    public boolean isLinear();

} // ~Expression
