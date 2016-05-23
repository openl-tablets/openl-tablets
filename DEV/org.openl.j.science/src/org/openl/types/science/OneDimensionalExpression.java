/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import java.util.Collections;
import java.util.Iterator;

/**
 * @author snshor
 * 
 */
public class OneDimensionalExpression extends AMultiplicativeExpression implements IDimensionPower {
    double scalar;
    IDimension dimension;

    public OneDimensionalExpression(double scalar, IDimension dimension) {
        this.scalar = scalar;
        this.dimension = dimension;
    }

    public IMultiplicativeExpression changeScalar(double newScalar) {
        return new OneDimensionalExpression(newScalar, dimension);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.science2.IDimensionPower#getDimension()
     */
    public IDimension getDimension() {
        return dimension;
    }

    public int getDimensionCount() {
        return 1;
    }

    public IDimensionPower getDimensionPower(IDimension id) {
        return id == dimension ? this : null;
    }

    public Iterator<IDimensionPower> getDimensionsPowers() {
        return Collections.<IDimensionPower>singletonList(this).iterator();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.science2.IDimensionPower#getPower()
     */
    public int getPower() {
        return 1;
    }

    public double getScalar() {
        return scalar;
    }

}
