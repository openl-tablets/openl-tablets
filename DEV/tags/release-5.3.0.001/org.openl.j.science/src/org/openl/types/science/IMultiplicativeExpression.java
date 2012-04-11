/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import java.util.Iterator;

/**
 * @author snshor
 *
 * Provides facility for expressing something like 15 m/s, or 3.5 APY
 */

public interface IMultiplicativeExpression {
    public IMultiplicativeExpression add(IMultiplicativeExpression im) throws RuntimeException;

    public IMultiplicativeExpression changeScalar(double newScalar);

    public IMultiplicativeExpression divide(IMultiplicativeExpression im);

    /**
     * Returns number of different dimensions it has
     *
     * @return
     */
    public int getDimensionCount();

    public IDimensionPower getDimensionPower(IDimension id);

    public Iterator getDimensionsPowers();

    /**
     * Return scalar part of the expression
     *
     * @return
     */
    public double getScalar();

    public IMultiplicativeExpression multiply(IMultiplicativeExpression im);

    public IMultiplicativeExpression negate();

    public String printAs(IMultiplicativeExpression asUnit, String image);

    public String printAs(IMultiplicativeExpression asUnit, String image, int doubleDidgits);

    public String printInSystem(IMeasurementSystem system, int doubleDigits);

    public IMultiplicativeExpression subtract(IMultiplicativeExpression im) throws RuntimeException;

}
