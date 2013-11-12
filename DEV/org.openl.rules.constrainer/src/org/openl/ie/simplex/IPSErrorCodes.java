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

public class IPSErrorCodes {
    /**
     * the interior point solution is undefined
     */
    static public final int LPX_T_UNDEF = native_LPX_T_UNDEF();
    /**
     * the interior point solution is optimal
     */
    static public final int LPX_T_OPT = native_LPX_T_OPT();

    static {
        Status.dictionary.put(new Integer(LPX_T_OPT), "ip solution is optimal");
        Status.dictionary.put(new Integer(LPX_T_UNDEF), "ip solution is undefined");
    }

    static public boolean isOptimal(int code) {
        return (code == LPX_T_OPT);
    }

    private static native int native_LPX_T_OPT();

    private static native int native_LPX_T_UNDEF();

    private IPSErrorCodes() {
    }
}