package org.openl.ie.exigensimplex.glpkimpl;

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

public class Algorithm {
    public static final int TWO_PHASED_REVISED_SIMPLEX = 0;
    public static final int INTERIOR_POINT = 1;

    static public boolean isAvailableAlgorithm(int alg) {
        switch (alg) {
            case TWO_PHASED_REVISED_SIMPLEX:
            case INTERIOR_POINT:
                return true;
            default:
                return false;
        }
    }

    static public int parseAlgCode(int code) {
        switch (code) {
            case TWO_PHASED_REVISED_SIMPLEX:
                return TWO_PHASED_REVISED_SIMPLEX;
            case INTERIOR_POINT:
                return INTERIOR_POINT;
            default:
                return TWO_PHASED_REVISED_SIMPLEX;
        }
    }
}