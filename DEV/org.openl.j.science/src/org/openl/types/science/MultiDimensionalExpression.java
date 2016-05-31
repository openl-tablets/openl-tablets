/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import java.util.Arrays;

/**
 * @author snshor
 * 
 */
public class MultiDimensionalExpression extends AMultiplicativeExpression {

    double scalar;
    IDimensionPower[] power;

    public MultiDimensionalExpression(double scalar, IDimensionPower[] power) {
        this.scalar = scalar;
        this.power = power;
    }

    public IMultiplicativeExpression changeScalar(double newScalar) {
        if (newScalar == scalar) {
            return this;
        }

        return new MultiDimensionalExpression(newScalar, power);
    }

    public int getDimensionCount() {
        return power.length;
    }

    public IDimensionPower getDimensionPower(IDimension id) {
        for (int i = 0; i < power.length; ++i) {
            if (power[i].getDimension() == id) {
                return power[i];
            }
        }
        return null;
    }

    public Iterable<IDimensionPower> getDimensionsPowers() {
        return Arrays.asList(power);
    }

    public double getScalar() {
        return scalar;
    }

}
