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

public class MIPAlgorithm {
    static public final int BRANCH_AND_BOUNDS = 0;

    static public boolean isAvailableAlgorithm(int alg) {
        if (alg == BRANCH_AND_BOUNDS) {
            return true;
        }
        return false;
    }

    private MIPAlgorithm() {
    }
}