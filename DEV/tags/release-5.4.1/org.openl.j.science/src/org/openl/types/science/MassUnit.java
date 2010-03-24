/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

/**
 * @author snshor
 *
 */
public class MassUnit extends ZeroBasedUnit {

    /**
     * @param dimension
     * @param normalMultiplier
     */
    public MassUnit(String name, double normalMultiplier) {
        super(name, Dimension.MASS, normalMultiplier);
    }

}
