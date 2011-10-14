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
public class TimeUnit extends ZeroBasedUnit {

    /**
     * @param dimension
     * @param normalMultiplier
     */
    public TimeUnit(String name, double normalMultiplier) {
        super(name, Dimension.TIME, normalMultiplier);
    }

}
