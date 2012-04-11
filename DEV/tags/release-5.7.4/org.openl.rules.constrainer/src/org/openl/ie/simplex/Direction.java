package org.openl.ie.simplex;

/**
 * <p>
 * Title: Direction
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

public class Direction {
    /**
     * minimization
     */
    public static final int MIN = native_LPX_MIN(); /* minimization */

    /**
     * maximization
     */
    public static final int MAX = native_LPX_MAX(); /* maximization */
    private static native int native_LPX_MAX();

    private static native int native_LPX_MIN();

    private Direction() {
    }
}