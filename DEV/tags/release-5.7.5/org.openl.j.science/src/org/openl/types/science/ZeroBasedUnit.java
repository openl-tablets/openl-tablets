/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

/**
 * @author snshor
 */

public class ZeroBasedUnit extends ASimpleUnit {
    double normalMultiplier;

    /**
     * @param dimension
     */
    public ZeroBasedUnit(String name, IDimension dimension, double normalMultiplier) {
        super(name, dimension);
        this.normalMultiplier = normalMultiplier;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.science2.IMultiplicativeExpression#changeScalar(double)
     */
    public IMultiplicativeExpression changeScalar(double newScalar) {
        if (newScalar == getScalar()) {
            return this;
        }
        return new OneDimensionalExpression(newScalar, getDimension());
    }

    @Override
    public double normalize(double value) {
        return normalMultiplier * value;
    }

}
