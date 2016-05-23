/*
 * Created on Jun 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

/**
 * @author snshor
 * 
 *         Provides facility for expressing something like 15 m/s, or 3.5 APY
 */

public interface IMultiplicativeExpression {
    IMultiplicativeExpression add(IMultiplicativeExpression im) throws RuntimeException;

    IMultiplicativeExpression changeScalar(double newScalar);

    IMultiplicativeExpression divide(IMultiplicativeExpression im);

    /**
     * Returns number of different dimensions it has
     * 
     * @return
     */
    int getDimensionCount();

    IDimensionPower getDimensionPower(IDimension id);

    Iterable<IDimensionPower> getDimensionsPowers();

    /**
     * Return scalar part of the expression
     * 
     * @return
     */
    double getScalar();

    IMultiplicativeExpression multiply(IMultiplicativeExpression im);

    IMultiplicativeExpression negate();

    String printAs(IMultiplicativeExpression asUnit, String image);

    String printAs(IMultiplicativeExpression asUnit, String image, int doubleDidgits);

    String printInSystem(IMeasurementSystem system, int doubleDigits);

    IMultiplicativeExpression subtract(IMultiplicativeExpression im) throws RuntimeException;

}
