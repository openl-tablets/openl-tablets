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

    static public final DistanceUnit mm = new DistanceUnit("mm", 0.001);
    static public final DistanceUnit cm = new DistanceUnit("cm", 0.01);
    static public final DistanceUnit m = new DistanceUnit("m", 1);
    static public final DistanceUnit km = new DistanceUnit("km", 1000);

    static public final DistanceUnit mi = new DistanceUnit("mi", 1609);
    static public final DistanceUnit in = new DistanceUnit("in", 0.0254);
    static public final DistanceUnit ft = new DistanceUnit("ft", 0.0254 * 12);
    static public final DistanceUnit yd = new DistanceUnit("yd", 0.0254 * 12 * 3);

    static public final TimeUnit mks = new TimeUnit("mks", 0.000001);
    static public final TimeUnit ms = new TimeUnit("ms", 0.001);
    static public final TimeUnit s = new TimeUnit("s", 1);
    static public final TimeUnit min = new TimeUnit("min", 60);
    static public final TimeUnit h = new TimeUnit("h", 3600);
    static public final TimeUnit day = new TimeUnit("day", 3600 * 24);
    static public final TimeUnit week = new TimeUnit("week", 3600 * 24 * 7);

    static public final MassUnit mg = new MassUnit("mg", 0.000001);
    static public final MassUnit g = new MassUnit("g", 0.001);
    static public final MassUnit kg = new MassUnit("kg", 1);
    static public final MassUnit t = new MassUnit("t", 1000);

    static public final MassUnit oz = new MassUnit("oz", 0.028);
    static public final MassUnit lb = new MassUnit("lb", 0.453592);

}
