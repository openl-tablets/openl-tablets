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

public class PrimalErrorCodes {
    /* status codes returned by getPrimStatus() */
    /**
     * primal status is undefined
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_P_UNDEF = native_LPX_P_UNDEF(); /*
                                                             * primal status is
                                                             * undefined
                                                             */
    /**
     * solution is primal feasible
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_P_FEAS = native_LPX_P_FEAS(); /*
                                                         * solution is primal
                                                         * feasible
                                                         */
    /**
     * solution is primal infeasible
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_P_INFEAS = native_LPX_P_INFEAS(); /*
                                                             * solution is
                                                             * primal infeasible
                                                             */
    /**
     * no primal feasible solution exist
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_P_NOFEAS = native_LPX_P_NOFEAS(); /*
                                                             * no primal
                                                             * feasible solution
                                                             * exists
                                                             */

    static {
        Status.dictionary.put(new Integer(LPX_P_UNDEF), "status is undefined");
        Status.dictionary.put(new Integer(LPX_P_FEAS), "solution is feasible");
        Status.dictionary.put(new Integer(LPX_P_INFEAS), "solution is primal infeasible");
        Status.dictionary.put(new Integer(LPX_P_NOFEAS), "no feasible solution exists");
    }

    /**
     * Checks wether an error code corresponds to those of the feasible solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isPrimalFeasible(int errorCode) {
        return ((errorCode == LPX_P_FEAS)) ? true : false;
    }

    private static native int native_LPX_P_FEAS();

    private static native int native_LPX_P_INFEAS();

    private static native int native_LPX_P_NOFEAS();

    private static native int native_LPX_P_UNDEF();

    private PrimalErrorCodes() {
    }
}