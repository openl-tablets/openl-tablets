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

public class MIPErrorCodes {
    /**
     * not a MIP problem
     */
    public static final int NOT_A_MIP = 3000;
    /**
     * integer status is undefined
     */
    public static final int LPX_I_UNDEF = native_LPX_I_UNDEF();
    /**
     * solution is integer optimal
     */
    public static final int LPX_I_OPT = native_LPX_I_OPT();
    /**
     * solution is integer feasible
     */
    public static final int LPX_I_FEAS = native_LPX_I_FEAS();
    /**
     * no integer solution exists
     */
    public static final int LPX_I_NOFEAS = native_LPX_I_NOFEAS();

    static {
        Status.dictionary.put(new Integer(LPX_I_UNDEF), "The problem status is undefined");
        Status.dictionary.put(new Integer(LPX_I_OPT), "The solution is integer optimal");
        Status.dictionary.put(new Integer(LPX_I_FEAS), "The solution is integer feasible");
        Status.dictionary.put(new Integer(LPX_I_NOFEAS), "The problem has no integer feasible solution");
        Status.dictionary.put(new Integer(NOT_A_MIP), "It is not a MIP kind problem");
    }

    /**
     * Checks wether an error code corresponds to those of the feasible solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isFeasible(int errorCode) {
        return ((errorCode == LPX_I_OPT) || (errorCode == LPX_I_FEAS)) ? true : false;
    }

    /**
     * Checks wether an error code corresponds to those of the optimal solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isOptimal(int errorCode) {
        return (errorCode == LPX_I_OPT) ? true : false;
    }

    private static native int native_LPX_I_FEAS();

    private static native int native_LPX_I_NOFEAS();

    private static native int native_LPX_I_OPT();

    private static native int native_LPX_I_UNDEF();

    private MIPErrorCodes() {
    }

}