/*
 * Created on Jun 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

/**
 * @author snshor
 *
 */
public interface IDerivedConstants extends IBasicConstants {
    IMultiplicativeExpression MPH = MI.divide(H);

    IMultiplicativeExpression M2 = M.multiply(M);

    IMultiplicativeExpression M3 = M.multiply(M.multiply(M));

    IMultiplicativeExpression L = CM.multiply(CM.multiply(CM)).changeScalar(1000);

    IMeasurementSystem METRIC = MeasurementSystem.METRIC;

}
