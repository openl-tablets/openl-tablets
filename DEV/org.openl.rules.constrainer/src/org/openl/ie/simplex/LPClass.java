package org.openl.ie.simplex;

/**
 * <p>
 * Title: LPClass
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company: Exigengroup
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class LPClass {

    /**
     * LP problem for the real variables
     */
    public static final int LPX_PURE = native_LPX_LP();

    /**
     * Mixed integer LP problem
     */
    public static final int LPX_MIP = native_LPX_MIP();
    static {
        Status.dictionary.put(new Integer(LPX_PURE), "Pure LP problem");
        Status.dictionary.put(new Integer(LPX_MIP), "Mixed integer programming problem");
    }

    private static native int native_LPX_LP();

    private static native int native_LPX_MIP();

    private LPClass() {
    }
}