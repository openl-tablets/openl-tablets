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
public class Operators implements IDerivedConstants {

    public static IMultiplicativeExpression index(double x, IMultiplicativeExpression m) {
        return m.changeScalar(m.getScalar() * x);
    }

}
