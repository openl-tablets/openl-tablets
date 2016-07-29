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
public class MeasurementSystem implements IMeasurementSystem, IBasicConstants {
    static class MetricSystem extends MeasurementSystem {

        public MetricSystem() {
            super("metric", KG, M, S, new MassUnit[] { T, KG, G, MG }, new DistanceUnit[] { KM, M, CM, MM },
                    new TimeUnit[] { WEEK, DAY, H, MIN, S, MS, MKS });
        }

    }

    public static final MeasurementSystem METRIC = new MetricSystem();
    private String name;
    private MassUnit baseMassUnit;

    private DistanceUnit baseDistanceUnit;
    private TimeUnit baseTimeUnit;
    private MassUnit[] massUnits;

    private DistanceUnit[] distanceUnits;

    private TimeUnit[] timeUnits;

    public MeasurementSystem(String name, MassUnit baseMassUnit, DistanceUnit baseDistanceUnit, TimeUnit baseTimeUnit,
            MassUnit[] massUnits, DistanceUnit[] distanceUnits, TimeUnit[] timeUnits) {
        this.name = name;
        this.baseMassUnit = baseMassUnit;
        this.baseDistanceUnit = baseDistanceUnit;
        this.baseTimeUnit = baseTimeUnit;
        this.massUnits = massUnits;
        this.timeUnits = timeUnits;
        this.distanceUnits = distanceUnits;
    }

    public DistanceUnit getBaseDistanceUnit() {
        return baseDistanceUnit;
    }

    public MassUnit getBaseMassUnit() {
        return baseMassUnit;
    }

    public TimeUnit getBaseTimeUnit() {
        return baseTimeUnit;
    }

    public String getDisplayName(int mode) {
        return name;
    }

    public DistanceUnit[] getDistanceUnits() {
        return distanceUnits;
    }

    public MassUnit[] getMassUnits() {
        return massUnits;
    }

    public String getName() {
        return name;
    }

    public TimeUnit[] getTimeUnits() {
        return timeUnits;
    }

    public String printExpression(IMultiplicativeExpression im, int doubleDigits) {

        IDimension[] supportedDimensions = { Dimension.DISTANCE, Dimension.TIME, Dimension.MASS };
        IUnit[] baseUnits = { baseDistanceUnit, baseTimeUnit, baseMassUnit };

        int dimCount = 0;
        int negCount = 0;
        int posCount = 0;
        IDimensionPower[] powers = new IDimensionPower[supportedDimensions.length];

        IMultiplicativeExpression expr = new ScalarExpression(1);

        for (int i = 0; i < supportedDimensions.length; i++) {
            IDimensionPower dp = im.getDimensionPower(supportedDimensions[i]);
            if (dp == null) {
                continue;
            }

            ++dimCount;
            if (dp.getPower() < 0) {
                ++negCount;
                for (int j = 0; j < Math.abs(dp.getPower()); ++j) {
                    expr = expr.divide(baseUnits[i]);
                }
            } else {
                ++posCount;
                for (int j = 0; j < dp.getPower(); ++j) {
                    expr = expr.multiply(baseUnits[i]);
                }
            }
            powers[i] = dp;
        }

        if (dimCount == 0) {
            return AMultiplicativeExpression.print(im, expr, "", doubleDigits);
        }

        StringBuilder buf = new StringBuilder();

        if (posCount == 0) {
            buf.append("1");
        } else {
            boolean printed = false;
            for (int i = 0; i < powers.length; i++) {
                if (powers[i] == null) {
                    continue;
                }
                int p = powers[i].getPower();
                if (p <= 0) {
                    continue;
                }
                if (printed) {
                    buf.append('*');
                }
                printed = true;
                buf.append(baseUnits[i].getName());
                if (p > 1) {
                    buf.append("^").append(p);
                }

            }
        }

        if (negCount != 0) {
            buf.append('/');
            boolean printed = false;
            for (int i = 0; i < powers.length; i++) {
                if (powers[i] == null) {
                    continue;
                }
                int p = powers[i].getPower();
                if (p >= 0) {
                    continue;
                }
                if (printed) {
                    buf.append('*');
                }
                printed = true;
                buf.append(baseUnits[i].getName());
                if (p > 1) {
                    buf.append("^").append(-p);
                }

            }
        }

        return AMultiplicativeExpression.print(im, expr, buf.toString(), doubleDigits);
    }

}
