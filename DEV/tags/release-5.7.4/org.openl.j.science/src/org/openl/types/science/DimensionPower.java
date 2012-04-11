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
public class DimensionPower implements IDimensionPower {
    IDimension dimension;
    int power;

    public DimensionPower(IDimension dimension, int power) {
        this.dimension = dimension;
        this.power = power;
    }

    public IDimension getDimension() {
        return dimension;
    }

    public int getPower() {
        return power;
    }

}
