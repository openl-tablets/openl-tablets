package org.openl.ie.exigensimplex;

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

public interface VariableType {
    /**
     * The variable is bounded above
     */
    int BOUNDED_ABOVE = 0;
    /**
     * the variable is bounded below
     */
    int BOUNDED_BELOW = 1;
    /**
     * The variable is bounded on an interval
     */
    int DOUBLE_BOUNDED = 2;
    /**
     * Unbounded variable
     */
    int FREE_VARIABLE = 3;
    /**
     * Fixed variable
     */
    int FIXED_VARIABLE = 4;
}