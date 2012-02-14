/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import java.util.Iterator;

import org.openl.util.AOpenIterator;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class ScalarExpression extends AMultiplicativeExpression {
    double scalar;

    public ScalarExpression(double scalar) {
        this.scalar = scalar;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.science2.IMultiplicativeExpression#changeScalar(double)
     */
    public IMultiplicativeExpression changeScalar(double newScalar) {
        if (newScalar == scalar) {
            return this;
        }

        return new ScalarExpression(newScalar);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.science2.IMultiplicativeExpression#getDimensionCount()
     */
    public int getDimensionCount() {
        return 0;
    }

    public IDimensionPower getDimensionPower(IDimension id) {
        return null;
    }

    public Iterator getDimensionsPowers() {
        return AOpenIterator.EMPTY;
    }

    public double getScalar() {
        return scalar;
    }

}
