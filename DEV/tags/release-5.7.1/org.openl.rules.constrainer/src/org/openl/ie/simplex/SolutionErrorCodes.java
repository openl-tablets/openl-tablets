package org.openl.ie.simplex;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class SolutionErrorCodes {
    /* exit codes returned by the simplex-based solver routines: */
    /**
     * The LP problem has been succefully solved to optimality
     */
    static public final int LPX_E_OK = native_LPX_E_OK(); /**/
    /**
     * empty problem
     */
    static public final int LPX_E_EMPTY = native_LPX_E_EMPTY(); /**/
    /**
     * invalid initial basis
     */
    static public final int LPX_E_BADB = native_LPX_E_BADB(); /**/
    /**
     * infeasible initial solution
     */
    static public final int LPX_E_INFEAS = native_LPX_E_INFEAS(); /**/
    /**
     * The solver can't start the search because either the problem has no rows
     * or columns or some rows has non zero objective coefficients
     */
    static public final int LPX_E_FAULT = native_LPX_E_FAULT(); /**/
    /**
     * objective lower limit reached
     */
    static public final int LPX_E_OBJLL = native_LPX_E_OBJLL(); /**/
    /**
     * objective upper limit reached
     */
    static public final int LPX_E_OBJUL = native_LPX_E_OBJUL(); /**/
    /**
     * The search was prematurely terrminated due to iterations limit being
     * exceeded
     */
    static public final int LPX_E_ITLIM = native_LPX_E_ITLIM(); /**/
    /**
     * time limit exhausted
     */
    static public final int LPX_E_TMLIM = native_LPX_E_TMLIM(); /**/
    /**
     * The problem has no feasible solution
     */
    static public final int LPX_E_NOFEAS = native_LPX_E_NOFEAS(); /**/
    /**
     * The search was prematurely terminated due to numerical instability on
     * solving Newtonial system
     */
    static public final int LPX_E_INSTAB = native_LPX_E_INSTAB(); /**/
    /**
     * problems with basis matrix
     */
    static public final int LPX_E_SING = native_LPX_E_SING(); /**/
    /**
     * The search was terminated due to very slow convergence
     */
    static public final int LPX_E_NOCONV = native_LPX_E_NOCONV();

    static {
        Status.dictionary.put(new Integer(LPX_E_OK), "Success");
        Status.dictionary.put(new Integer(LPX_E_EMPTY), "Empty problem");
        Status.dictionary.put(new Integer(LPX_E_BADB), "Invalid initial basis");
        Status.dictionary.put(new Integer(LPX_E_INFEAS), "Infeasible initial solution");
        Status.dictionary.put(new Integer(LPX_E_FAULT), "Unable to start the search");
        Status.dictionary.put(new Integer(LPX_E_OBJLL), "Objective lower limit reached");
        Status.dictionary.put(new Integer(LPX_E_OBJUL), "Objective upper limit reached");
        Status.dictionary.put(new Integer(LPX_E_ITLIM), "Iterations limit exhausted");
        Status.dictionary.put(new Integer(LPX_E_TMLIM), "Time limit exhausted");
        Status.dictionary.put(new Integer(LPX_E_NOFEAS), "No feasible solution");
        Status.dictionary.put(new Integer(LPX_E_INSTAB), "Numerical instability");
        Status.dictionary.put(new Integer(LPX_E_SING), "Problems with basis matrix");
        Status.dictionary.put(new Integer(LPX_E_NOCONV), "very slow convergence");
    }

    static public boolean isSuccessful(int code) {
        if (code == LPX_E_OK) {
            return true;
        }
        return false;
    }

    private static native int native_LPX_E_BADB();

    private static native int native_LPX_E_EMPTY();

    private static native int native_LPX_E_FAULT();

    private static native int native_LPX_E_INFEAS();

    private static native int native_LPX_E_INSTAB();

    private static native int native_LPX_E_ITLIM();

    private static native int native_LPX_E_NOCONV();

    private static native int native_LPX_E_NOFEAS();

    private static native int native_LPX_E_OBJLL();

    private static native int native_LPX_E_OBJUL();

    private static native int native_LPX_E_OK();

    private static native int native_LPX_E_SING();

    private static native int native_LPX_E_TMLIM();
}