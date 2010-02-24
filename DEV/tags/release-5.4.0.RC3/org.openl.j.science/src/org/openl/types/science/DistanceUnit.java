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
public class DistanceUnit extends ZeroBasedUnit {

    /**
     * @param dimension
     * @param normalMultiplier
     */
    public DistanceUnit(String name, double normalMultiplier) {
        super(name, Dimension.DISTANCE, normalMultiplier);
    }

}
