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

public class DualErrorCodes {
    /* status codes returned by getDualStatus() */
    /**
     * dual status is undefined
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_D_UNDEF = native_LPX_D_UNDEF(); /*
                                                             * dual status is
                                                             * undefined
                                                             */
    /**
     * solution is dual feasible
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_D_FEAS = native_LPX_D_FEAS(); /*
                                                         * solution is dual
                                                         * feasible
                                                         */
    /**
     * solution is dual infeasible
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_D_INFEAS = native_LPX_D_INFEAS(); /*
                                                             * solution is dual
                                                             * infeasible
                                                             */
    /**
     * no dual feasible solution exist
     *
     * @see LPX#getPrimStatus()
     */
    static public int LPX_D_NOFEAS = native_LPX_D_NOFEAS(); /*
                                                             * no dual feasible
                                                             * solution exists
                                                             */

    static {
        Status.dictionary.put(new Integer(LPX_D_UNDEF), "status is undefined");
        Status.dictionary.put(new Integer(LPX_D_FEAS), "solution is feasible");
        Status.dictionary.put(new Integer(LPX_D_INFEAS), "solution is dual infeasible");
        Status.dictionary.put(new Integer(LPX_D_NOFEAS), "no feasible solution exists");
    }

    /**
     * Checks wether an error code corresponds to those of the feasible solution
     *
     * @param errorCode An error code to be checked
     * @return <code>true</code> if it does, <code>false</code> otherwise
     */
    public static boolean isDualFeasible(int errorCode) {
        return ((errorCode == LPX_D_FEAS)) ? true : false;
    }

    private static native int native_LPX_D_FEAS();

    private static native int native_LPX_D_INFEAS();

    private static native int native_LPX_D_NOFEAS();

    private static native int native_LPX_D_UNDEF();

    private DualErrorCodes() {
    }
}