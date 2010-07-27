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

public class VarKind {
    public static final int REAL_VAR = native_LPX_CV();

    public static final int INT_VAR = native_LPX_IV();
    private static native int native_LPX_CV();

    private static native int native_LPX_IV();

    private VarKind() {
    }
}