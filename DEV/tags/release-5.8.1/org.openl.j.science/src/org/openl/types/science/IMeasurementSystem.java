/*
 * Created on Jun 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import org.openl.base.INamedThing;

/**
 * @author snshor
 *
 */
public interface IMeasurementSystem extends INamedThing {
    DistanceUnit getBaseDistanceUnit();

    MassUnit getBaseMassUnit();

    TimeUnit getBaseTimeUnit();

    DistanceUnit[] getDistanceUnits();

    MassUnit[] getMassUnits();

    TimeUnit[] getTimeUnits();

    String printExpression(IMultiplicativeExpression expr, int doubleDigits);

}
