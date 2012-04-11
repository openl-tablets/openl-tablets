package org.openl.ie.constrainer.lpsolver;

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

public interface LPConstraint {
    public double getLb();

    public int[] getLocations();

    public int getType();

    public double getUb();

    public double[] getValues();
}