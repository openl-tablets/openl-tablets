/*
 * Created on Jun 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.science;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author snshor
 * 
 */
public abstract class AMultiplicativeExpression implements IMultiplicativeExpression {

    static IMultiplicativeExpression additiveOp(IMultiplicativeExpression m1, IMultiplicativeExpression m2, int sign) {
        if (!isAdditiveCompatible(m1, m2)) {
            throw new RuntimeException("" + m1 + " and " + m2 + " have different dimensions");
        }

        return m1.changeScalar(m1.getScalar() + sign * m2.getScalar());
    }

    public static boolean isAdditiveCompatible(IMultiplicativeExpression m1, IMultiplicativeExpression m2) {

        if (m1.getDimensionCount() != m2.getDimensionCount()) {
            return false;
        }

        for (IDimensionPower d1 : m1.getDimensionsPowers()) {
            IDimensionPower d2 = m2.getDimensionPower(d1.getDimension());

            if (d2 == null || d1.getPower() != d2.getPower()) {
                return false;
            }
        }

        return true;
    }

    static IMultiplicativeExpression merge(IMultiplicativeExpression m1, IMultiplicativeExpression m2, boolean multiply) {

        List<IDimensionPower> res = new ArrayList<IDimensionPower>();

        for (IDimensionPower d1 : m1.getDimensionsPowers()) {
            IDimensionPower d2 = m2.getDimensionPower(d1.getDimension());

            if (d2 == null) {
                res.add(d1);
            } else {
                int newPower = d1.getPower() + (multiply ? d2.getPower() : -d2.getPower());

                if (newPower != 0) {
                    res.add(new DimensionPower(d1.getDimension(), newPower));
                }
            }
        }

        for (IDimensionPower d2 : m2.getDimensionsPowers()) {
            IDimensionPower d1 = m1.getDimensionPower(d2.getDimension());

            if (d1 == null) {
                res.add(multiply ? d2 : negative(d2));
            }
        }

        double newScalar = multiply ? m1.getScalar() * m2.getScalar() : m1.getScalar() / m2.getScalar();

        if (res.size() == 0) {
            return new ScalarExpression(newScalar);
        }

        return new MultiDimensionalExpression(newScalar,
                (IDimensionPower[]) res.toArray(new IDimensionPower[res.size()]));
    }

    static IDimensionPower negative(IDimensionPower dp) {
        return new DimensionPower(dp.getDimension(), -dp.getPower());
    }

    public static String print(IMultiplicativeExpression me, IMultiplicativeExpression asUnit, String unitImage,
            int doubleDidgits) {
        if (!isAdditiveCompatible(me, asUnit)) {
            throw new RuntimeException("Bad unit for printing");
        }

        IMultiplicativeExpression ie = me.divide(asUnit);

        NumberFormat format = NumberFormat.getInstance();
        format.setMaximumFractionDigits(doubleDidgits);

        return format.format(ie.getScalar()) + unitImage;

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.types.science.IMultiplicativeExpression#add(org.openl.types
     * .science.IMultiplicativeExpression)
     */
    public IMultiplicativeExpression add(IMultiplicativeExpression im) throws RuntimeException {
        return additiveOp(this, im, 1);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.types.science.IMultiplicativeExpression#divide(org.openl.types
     * .science.IMultiplicativeExpression)
     */
    public IMultiplicativeExpression divide(IMultiplicativeExpression im) {
        return merge(this, im, false);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.types.science.IMultiplicativeExpression#multiply(org.openl.
     * types.science.IMultiplicativeExpression)
     */
    public IMultiplicativeExpression multiply(IMultiplicativeExpression im) {
        return merge(this, im, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openl.types.science.IMultiplicativeExpression#negate()
     */
    public IMultiplicativeExpression negate() {
        return changeScalar(-getScalar());
    }

    public String printAs(IMultiplicativeExpression asUnit, String image) {
        return print(this, asUnit, image, 2);
    }

    public String printAs(IMultiplicativeExpression asUnit, String image, int doubleDidgits) {
        return print(this, asUnit, image, doubleDidgits);
    }

    public String printInSystem(IMeasurementSystem system, int doubleDigits) {
        return system.printExpression(this, doubleDigits);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.openl.types.science.IMultiplicativeExpression#subtract(org.openl.
     * types.science.IMultiplicativeExpression)
     */
    public IMultiplicativeExpression subtract(IMultiplicativeExpression im) throws RuntimeException {
        return additiveOp(this, im, -1);
    }

    @Override
    public String toString() {
        return printInSystem(MeasurementSystem.METRIC, 2);
    }

}
