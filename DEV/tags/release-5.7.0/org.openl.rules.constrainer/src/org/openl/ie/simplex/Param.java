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

public class Param {
    static private double DBL_EPSILON = native_DBL_EPSILON();
    static private int[] params = new int[250];

    /**
     * level of messages output by the GLPK's solver:
     * <ul>
     * <li> 0 -- no output
     * <li> 1 -- error messages only
     * <li> 2 -- normal output
     * <li> 3 -- full output (includes informational messages)
     * </ul>
     * Default value is 3
     */
    static public final int LPX_K_MSGLEV = 1;

    // static private double REAL_PARM_MIN = native_DBL_EPSILON();
    // static private double REAL_PARM_MAX = native_REAL_PARM_MAX();

    /**
     * Scaling options
     * <ul>
     * <li> 0 -- no scaling
     * <li> 1 -- equilibration scaling
     * <li> 2 -- geometric mean scaling
     * <li> 3 -- both geometric and equilibration scaling are applied in turn.
     * </ul>
     * Default value is 3
     */
    static public final int LPX_K_SCALE = 2;
    /**
     * Dual simplex option
     * <ul>
     * <li> 0 -- don't use the dual simplex
     * <li> 1 -- if initial basic solution is dual feasible, use the dual
     * simplex
     * </ul>
     * Default value is zero
     */
    static public final int LPX_K_DUAL = 3;
    /**
     * pricing option (for both primal and dual simplex):
     * <ul>
     * <li> 0 -- textbook pricing
     * <li> 1 -- steepest edge pricing
     * </ul>
     * Default is 1
     */
    static public final int LPX_K_PRICE = 4;
    /**
     * relative tolerance used to check if the current basic solution is primal
     * feasible
     */
    static public final int LPX_K_TOLBND = 5;
    /**
     * absolute tolerance used to check if the current basic solution is dual
     * feasible
     */
    static public final int LPX_K_TOLDJ = 6;
    /**
     * relative tolerance used to choose eligible pivotal elements of the
     * simplex table in the ratio test
     */
    static public final int LPX_K_TOLPIV = 7;
    /**
     * solution rounding option:
     * <ul>
     * <li> 0 -- report all computed values and reduced costs "as is"
     * <li> 1 -- if possible (allowed by the tolerances), replace computed
     * values and reduced costs which are close to zero by exact zeros
     * </ul>
     */
    static public final int LPX_K_ROUND = 8;
    /**
     * <ul>
     * <li> lower limit of the objective function; if on the phase II the
     * <li> objective function reaches this limit and continues decreasing,
     * <li> the solver stops the search
     * </ul>
     */
    static public final int LPX_K_OBJLL = 9;
    /**
     * upper limit of the objective function; if on the phase II the objective
     * function reaches this limit and continues increasing, the solver stops
     * the search.
     */
    static public final int LPX_K_OBJUL = 10;
    /**
     * simplex iterations limit; if this value is positive, it is decreased by
     * one each time when one simplex iteration has been performed, and reaching
     * zero value signals the solver to stop the search; negative value means no
     * iterations limit
     */
    static public final int LPX_K_ITLIM = 11;
    /**
     * simplex iterations count; this count is increased by one each time when
     * one simplex iteration has been performed
     */
    static public final int LPX_K_ITCNT = 12;
    /**
     * searching time limit, in seconds; if this value is positive, it is
     * decreased each time when one simplex iteration has been performed by the
     * amount of time spent for the iteration, and reaching zero value signals
     * the solver to stop the search; negative value means no time limit
     */
    static public final int LPX_K_TMLIM = 13;
    /**
     * output frequency, in iterations; this parameter specifies how frequently
     * the solver sends information about the solution to the standard output
     */
    static public final int LPX_K_OUTFRQ = 14;
    /**
     * output delay, in seconds; this parameter specifies how long the solver
     * should delay sending information about the solution to the standard
     * output; zero value means no delay
     */
    static public final int LPX_K_OUTDLY = 15;
    /**
     * branching heuristic:
     * <ul>
     * <li> 0 -- branch on the first variable
     * <li> 1 -- branch on the last variable
     * <li> 2 -- branch using a heuristic by Driebeck and Tomlin
     * </ul>
     */
    static public final int LPX_K_BRANCH = 16;
    /**
     * for MIP backtracking heuristic:
     * <ul>
     * <li> 0 -- depth first search
     * <li> 1 -- breadth first search
     * <li> 2 -- backtrack using the best projection heuristic
     * </ul>
     */
    static public final int LPX_K_BTRACK = 17;
    /**
     * absolute tolerance used to check if the current basic solution is integer
     * feasible
     */
    static public final int LPX_K_TOLINT = 18;
    /**
     * relative tolerance used to check if the value of the objective function
     * is not better than in the best known integer feasible solution
     */
    static public final int LPX_K_TOLOBJ = 19;
    /**
     * This parameter tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * how to output the objective function row:
     * <ul>
     * <li> 1 - never output objective function row
     * <li> 2 - always output objective function row
     * <li> 3 - output objective function row if the problem doesn't have any
     * free rows
     * <ul>
     * <b>Default value is 2</b>
     */
    static public final int LPX_K_MPSOBJ = 20;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to add some special comments
     */
    static public final int LPX_K_MPSINFO = 21;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to use original symbolic names of rows and columns, otherwise the routine
     * generates plain names using ordinal numbers of rows and columns.
     * <b>Default value is 0</b>
     */
    static public final int LPX_K_MPSORIG = 22;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to use all data field otherwise it keeps the fields 5 and 6 empty
     * <b>Default value is 1</b>
     */
    static public final int LPX_K_MPSWIDE = 23;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to omit column and vectors names every time when possible, otherwise the
     * routine never omits them. <b>Default value is 0</b>
     */
    static public final int LPX_K_MPSFREE = 24;

    /**
     * If set the flag tells the routine {@link LPX#saveLPtoMPSFormat(String)}
     * to skip empty columns <b>Default value is 0</b>
     */
    static public final int LPX_K_MPSSKIP = 25;

    static public final int LPX_K_RELAX = 26;

    /**
     * total amount of currently allocated memory in bytes
     */
    static public final int LPX_K_MEMCNT = 100;

    static {
        params[LPX_K_MSGLEV] = native_LPX_K_MSGLEV();
        params[LPX_K_SCALE] = native_LPX_K_SCALE();
        params[LPX_K_ITLIM] = native_LPX_K_ITLIM();
        params[LPX_K_ITCNT] = native_LPX_K_ITCNT();
        params[LPX_K_OUTFRQ] = native_LPX_K_OUTFRQ();
        params[LPX_K_BRANCH] = native_LPX_K_BRANCH();
        params[LPX_K_BTRACK] = native_LPX_K_BTRACK();
        params[LPX_K_MPSOBJ] = native_LPX_K_MPSOBJ();
        params[LPX_K_DUAL] = native_LPX_K_DUAL();
        params[LPX_K_PRICE] = native_LPX_K_PRICE();
        params[LPX_K_ROUND] = native_LPX_K_ROUND();
        params[LPX_K_MPSINFO] = native_LPX_K_MPSINFO();
        params[LPX_K_MPSORIG] = native_LPX_K_MPSORIG();
        params[LPX_K_MPSWIDE] = native_LPX_K_MPSWIDE();
        params[LPX_K_MPSFREE] = native_LPX_K_MPSFREE();
        params[LPX_K_MPSSKIP] = native_LPX_K_MPSSKIP();
        // params[LPX_K_LPTORIG]= native_LPX_K_LPTORIG();
        // params[LPX_K_RELAX] = native_LPX_K_RELAX();
        params[LPX_K_TOLBND] = native_LPX_K_TOLBND();
        params[LPX_K_TOLDJ] = native_LPX_K_TOLDJ();
        params[LPX_K_TOLPIV] = native_LPX_K_TOLPIV();
        params[LPX_K_OBJLL] = native_LPX_K_OBJLL();
        params[LPX_K_OBJUL] = native_LPX_K_OBJUL();
        params[LPX_K_TMLIM] = native_LPX_K_TMLIM();
        params[LPX_K_OUTDLY] = native_LPX_K_OUTDLY();
        params[LPX_K_TOLINT] = native_LPX_K_TOLINT();
        params[LPX_K_TOLOBJ] = native_LPX_K_TOLOBJ();
    }

    static public int getParam(int code) {
        int param = params[code];
        if (param == 0) {
            throw new IllegalArgumentException("Unexpected parameter's code: " + code);
        }
        return param;
    }

    static public boolean isValidBoolParam(int parm) {
        switch (parm) {
            case LPX_K_DUAL:
            case LPX_K_PRICE:
            case LPX_K_ROUND:
            case LPX_K_MPSINFO:
            case LPX_K_MPSORIG:
            case LPX_K_MPSWIDE:
            case LPX_K_MPSFREE:
            case LPX_K_MPSSKIP:
                // case LPX_K_LPTORIG:
                return true;
            default:
                return false;
        }
    }

    static public boolean isValidIntParam(int parm) {
        switch (parm) {
            case LPX_K_MSGLEV:
            case LPX_K_SCALE:
            case LPX_K_ITLIM:
            case LPX_K_ITCNT:
            case LPX_K_OUTFRQ:
            case LPX_K_BRANCH:
            case LPX_K_BTRACK:
            case LPX_K_MPSOBJ:
                return true;
            default:
                return false;
        }
    }

    static public boolean isValidIntParam(int parm, int val) {
        switch (parm) {
            case LPX_K_MSGLEV:
                if (!(0 <= val && val <= 3)) {
                    return false;
                } else {
                    return true;
                }

            case LPX_K_SCALE:
                if (!(0 <= val && val <= 3)) {
                    return false;
                } else {
                    return true;
                }

            case LPX_K_ITLIM:
                return true;

            case LPX_K_ITCNT:
                return true;

            case LPX_K_OUTFRQ:
                if (!(val > 0)) {
                    return false;
                } else {
                    return true;
                }

            case LPX_K_BRANCH:
                if (!(val == 0 || val == 1 || val == 2)) {
                    return false;
                } else {
                    return true;
                }

            case LPX_K_BTRACK:
                if (!(val == 0 || val == 1 || val == 2)) {
                    return false;
                } else {
                    return true;
                }

            case LPX_K_MPSOBJ:
                if (!(val == 0 || val == 1 || val == 2)) {
                    return false;
                } else {
                    return true;
                }

            default:
                return false;
        }
    }

    static public boolean isValidRealParam(int parm, double val) {
        switch (parm) {
            case LPX_K_RELAX:
                if (!(0.0 <= val && val <= 1.0)) {
                    return false;
                } else {
                    return true;
                }
            case LPX_K_TOLBND:
                if (!(DBL_EPSILON <= val && val <= 0.001)) {
                    return false;
                } else {
                    return true;
                }
            case LPX_K_TOLDJ:
                if (!(DBL_EPSILON <= val && val <= 0.001)) {
                    return false;
                } else {
                    return true;
                }
            case LPX_K_TOLPIV:
                if (!(DBL_EPSILON <= val && val <= 0.001)) {
                    return false;
                } else {
                    return true;
                }
            case LPX_K_OBJLL:
                return true;
            case LPX_K_OBJUL:
                return true;
            case LPX_K_TMLIM:
                return true;
            case LPX_K_OUTDLY:
                return true;
            case LPX_K_TOLINT:
                if (!(DBL_EPSILON <= val && val <= 0.001)) {
                    return false;
                } else {
                    return true;
                }
            case LPX_K_TOLOBJ:
                if (!(DBL_EPSILON <= val && val <= 0.001)) {
                    return false;
                } else {
                    return true;
                }
            default:
                return false;
        }
    }

    static public boolean isValidRealParm(int parm) {
        switch (parm) {
            case LPX_K_RELAX:
            case LPX_K_TOLBND:
            case LPX_K_TOLDJ:
            case LPX_K_TOLPIV:
            case LPX_K_OBJLL:
            case LPX_K_OBJUL:
            case LPX_K_TMLIM:
            case LPX_K_OUTDLY:
            case LPX_K_TOLINT:
            case LPX_K_TOLOBJ:
                return true;
            default:
                return false;
        }
    }

    private static native double native_DBL_EPSILON();

    private static native int native_LPX_K_BRANCH();

    private static native int native_LPX_K_BTRACK();

    private static native int native_LPX_K_DUAL();

    private static native int native_LPX_K_ITCNT();

    private static native int native_LPX_K_ITLIM();

    private static native int native_LPX_K_MPSFREE();

    private static native int native_LPX_K_MPSINFO();

    private static native int native_LPX_K_MPSOBJ();

    private static native int native_LPX_K_MPSORIG();

    private static native int native_LPX_K_MPSSKIP();

    private static native int native_LPX_K_MPSWIDE();

    private static native int native_LPX_K_MSGLEV();

    private static native int native_LPX_K_OBJLL();

    private static native int native_LPX_K_OBJUL();

    private static native int native_LPX_K_OUTDLY();

    private static native int native_LPX_K_OUTFRQ();

    private static native int native_LPX_K_PRICE();

    private static native int native_LPX_K_ROUND();

    private static native int native_LPX_K_SCALE();

    private static native int native_LPX_K_TMLIM();

    private static native int native_LPX_K_TOLBND();

    private static native int native_LPX_K_TOLDJ();

    private static native int native_LPX_K_TOLINT();

    private static native int native_LPX_K_TOLOBJ();

    private static native int native_LPX_K_TOLPIV();

    private Param() {
    }
}