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
public interface IBasicConstants {

    /**
     * <href="http://ts.nist.gov/ts/htdocs/230/235/appxc/appxc.htm"/>
     */

    DistanceUnit MM = new DistanceUnit("mm", 0.001);
    DistanceUnit CM = new DistanceUnit("cm", 0.01);
    DistanceUnit M = new DistanceUnit("m", 1);
    DistanceUnit KM = new DistanceUnit("km", 1000);

    DistanceUnit MI = new DistanceUnit("mi", 1609);
    DistanceUnit IN = new DistanceUnit("in", 0.0254);
    DistanceUnit FT = new DistanceUnit("ft", 0.0254 * 12);
    DistanceUnit YD = new DistanceUnit("yd", 0.0254 * 12 * 3);

    TimeUnit MKS = new TimeUnit("mks", 0.000001);
    TimeUnit MS = new TimeUnit("ms", 0.001);
    TimeUnit S = new TimeUnit("s", 1);
    TimeUnit MIN = new TimeUnit("min", 60);
    TimeUnit H = new TimeUnit("h", 3600);
    TimeUnit DAY = new TimeUnit("day", 3600 * 24);
    TimeUnit WEEK = new TimeUnit("week", 3600 * 24 * 7);

    MassUnit mg = new MassUnit("mg", 0.000001);
    MassUnit g = new MassUnit("g", 0.001);
    MassUnit kg = new MassUnit("kg", 1);
    MassUnit t = new MassUnit("t", 1000);

    MassUnit oz = new MassUnit("oz", 0.028);
    MassUnit lb = new MassUnit("lb", 0.453592);

}
