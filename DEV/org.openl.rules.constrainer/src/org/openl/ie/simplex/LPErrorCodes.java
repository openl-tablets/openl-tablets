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

public class LPErrorCodes {
    /* status codes reported by the routine lpx_get_status: */
    static public final int NO_LP = 3001;
    /**
     * optimal solution
     */
    static public final int LPX_OPT = native_LPX_OPT(); /**/
    /**
     * The solution is feasible
     */
    static public final int LPX_FEAS = native_LPX_FEAS(); /**/
    /**
     * The solution is infeasible
     */
    static public final int LPX_INFEAS = native_LPX_INFEAS(); /**/
    /**
     * There is no feasible solution
     */
    static public final int LPX_NOFEAS = native_LPX_NOFEAS(); /**/
    /**
     * The solution is unbounded
     */
    static public final int LPX_UNBND = native_LPX_UNBND(); /**/
    /**
     * The solution status is undefined
     */
    static public final int LPX_UNDEF = native_LPX_UNDEF(); /**/

    static {
        Status.dictionary.put(new Integer(NO_LP), "LP problem hasn't been created yet");
        Status.dictionary.put(new Integer(LPX_OPT), "OPTIMAL");
        Status.dictionary.put(new Integer(LPX_FEAS), "FEASIBLE");
        Status.dictionary.put(new Integer(LPX_INFEAS), "INFEASIBLE (INTERMEDIATE)");
        Status.dictionary.put(new Integer(LPX_NOFEAS), "INFEASIBLE (FINAL)");
        Status.dictionary.put(new Integer(LPX_UNBND), "UNBOUNDED");
        Status.dictionary.put(new Integer(LPX_UNDEF), "UNDEFINED");
    }

    /**
     * Checks wether an error code corresponds to those of the feasible solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isFeasible(int errorCode) {
        return ((errorCode == LPX_OPT) || (errorCode == LPX_FEAS)) ? true : false;
    }

    /**
     * Checks wether an error code corresponds to those of the optimal solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isOptimal(int errorCode) {
        return (errorCode == LPX_OPT) ? true : false;
    }

    private static native int native_LPX_FEAS();

    private static native int native_LPX_INFEAS();

    private static native int native_LPX_NOFEAS();

    private static native int native_LPX_OPT();

    private static native int native_LPX_UNBND();

    private static native int native_LPX_UNDEF();

    private LPErrorCodes() {
    }
}