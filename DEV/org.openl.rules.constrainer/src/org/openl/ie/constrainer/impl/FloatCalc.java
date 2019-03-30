package org.openl.ie.constrainer.impl;

import org.openl.ie.constrainer.Constrainer;
import org.openl.ie.constrainer.Failure;
import org.openl.ie.constrainer.FloatExp;

//  IEEE 754 NAN and inf RULES
//  - result of any operation with the operand NAN = NAN
//  - (-inf)+(+inf) = NAN
//  - (+inf)-(+inf) = NAN
//  - (-inf)-(-inf) = NAN
//  - inf*0 = NAN
//  - inf/inf = NAN
//  - 0/0 = NAN ((!0)/0 = (+/-)inf)
//
// We have to try the rules:
//   - values can be finite doubles, +inf, -inf but not NaN

/**
 * A helper for the floating-point arithmetic.
 */
public final class FloatCalc {
    /**
     * A Not-a-Number (NaN) value of type <code>double</code>.
     *
     * @see Double#NaN
     */
    static public final double NaN = Double.NaN;

    /**
     * The positive infinity of type <code>double</code>.
     *
     * @see Double#POSITIVE_INFINITY
     */
    static public final double pInf = Double.POSITIVE_INFINITY;

    /**
     * The negative infinity of type <code>double</code>.
     *
     * @see Double#NEGATIVE_INFINITY
     */
    static public final double nInf = Double.NEGATIVE_INFINITY;

    public static void assertMinMax(double min, double max) {
        doAssert(!isNan(min), "min not NaN");
        doAssert(!isNan(max), "max not NaN");
        doAssert(min <= max, "min <= max");
    }

    /**
     * Returns the expression: <code>log(x,base)</code>.
     */
    public static double calc_log(double x, double base) {
        // log(x,e)=log(x,base)*log(base,e)
        return Math.log(x) / Math.log(base);
    }

    public static void doAssert(boolean v, String s) {
        if (v) {
            return;
        }
        System.out.println("Assertion failed: " + s);
    }

    /**
     * Returns true if the specified numbers are equal with the precision
     */
    public static boolean eq(double d1, double d2) {
        return Math.abs(d1 - d2) < Constrainer.FLOAT_PRECISION;
    }

    /**
     * Returns true if the first number is more or equal to the second number with the precision
     */
    public static boolean ge(double d1, double d2) {
        return d1 - d2 > -Constrainer.FLOAT_PRECISION;
    }

    /**
     * Returns true if the first number more than the second number with the precision
     */
    public static boolean gt(double d1, double d2) {
        return (d1 - d2) > Constrainer.FLOAT_PRECISION;
    }

    /**
     * Returns the expression: <code>max(1 / [v..v])</code>. Result may be finite number or +inf.
     */
    public static double inverseMax(double v) {
        return v != 0 ? 1 / v : pInf;
    }

    /**
     * Returns the expression: <code>max(1 / [min..max])</code>. Result may be finite number or +inf.
     */
    public static double inverseMax(double min, double max) {
        assertMinMax(min, max);
        double _max;
        // strictly positive or strictly negative
        if (min > 0 || max < 0) {
            _max = 1 / min;
        }
        // [0..>0]
        else if (min == 0 && max > 0) {
            _max = pInf;
        }
        // [<0..0]
        else if (max == 0 && min < 0) {
            _max = 1 / min;
        }
        // [<0..>0] or [0..0]
        else {
            _max = pInf;
        }
        return _max;
    }

    /**
     * Returns the expression: <code>min(1 / [v..v])</code>. Result may be finite number or -inf.
     */
    public static double inverseMin(double v) {
        return v != 0 ? 1 / v : nInf;
    }

    /**
     * Returns the expression: <code>min(1 / [min..max])</code>. Result may be finite number or -inf.
     */
    public static double inverseMin(double min, double max) {
        assertMinMax(min, max);
        double _min;
        // strictly positive or strictly negative
        if (min > 0 || max < 0) {
            _min = 1 / max;
        }
        // [0..>0]
        else if (min == 0 && max > 0) {
            _min = 1 / max;
        }
        // [<0..0]
        else if (max == 0 && min < 0) {
            _min = nInf;
        }
        // [<0..>0] or [0..0]
        else {
            _min = nInf;
        }
        return _min;
    }

    /**
     * Returns true if the specified number is infinitely large in magnitude.
     *
     * @see Double#isInfinite
     */
    public static boolean isInf(double v) {
        return Double.isInfinite(v);
    }

    /**
     * Returns true if the specified number is the special Not-a-Number (NaN) value.
     *
     * @see Double#isNaN
     */
    public static boolean isNan(double v) {
        return Double.isNaN(v);
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code>.
     */
    public static double productMax(double min1, double max1, double min2, double max2) {
        // exp1 >= 0
        if (min1 >= 0) {
            return productMaxP(min1, max1, max2);
        } else if (max1 <= 0) {
            return productMaxN(min1, max1, min2);
        } else {
            // exp2 >= 0
            if (min2 >= 0) {
                return max1 * max2;
            } else if (max2 <= 0) {
                return min1 * min2;
            } else {
                return Math.max(min1 * min2, max1 * max2);
            }
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code> where <code>[min1..max1] <= 0</code>.
     */
    public static double productMaxN(double min1, double max1, double min2) {
        if (min2 >= 0) {
            return max1 * min2;
        } else {
            return min1 * min2;
        }
    }

    /**
     * Returns the expression: <code>max([min1..max1]*[min2..max2])</code> where <code>[min1..max1] >= 0</code>.
     */
    public static double productMaxP(double min1, double max1, double max2) {
        if (max2 >= 0) {
            return max1 * max2;
        } else {
            return min1 * max2;
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code>.
     */
    public static double productMin(double min1, double max1, double min2, double max2) {
        // exp1 >= 0
        if (min1 >= 0) {
            return productMinP(min1, max1, min2);
        } else if (max1 <= 0) {
            return productMinN(min1, max1, max2);
        } else {
            // exp2 >= 0
            if (min2 >= 0) {
                return min1 * max2;
            } else if (max2 <= 0) {
                return max1 * min2;
            } else {
                return Math.min(max1 * min2, min1 * max2);
            }
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code> where <code>[min1..max1] <= 0</code>.
     */
    public static double productMinN(double min1, double max1, double max2) {
        if (max2 >= 0) {
            return min1 * max2;
        } else {
            return max1 * max2;
        }
    }

    /**
     * Returns the expression: <code>min([min1..max1]*[min2..max2])</code> where <code>[min1..max1] >= 0</code>.
     */
    public static double productMinP(double min1, double max1, double min2) {
        if (min2 >= 0) {
            return min1 * min2;
        } else {
            return max1 * min2;
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>max([min1..max1]*[min2..max2]) <= max</code>.
     */
    static public void productSetMax(double max, FloatExp exp1, FloatExp exp2) throws Failure {
        double min1, max1;
        // exp1 >= 0
        if ((min1 = exp1.min()) >= 0) {
            productSetMaxP(max, min1, exp1.max(), exp2);
        }
        // exp1 <= 0
        else if ((max1 = exp1.max()) <= 0) {
            productSetMaxN(max, min1, max1, exp2);
        } else // exp1 changes sign
        {
            if (max < 0) {
                double m = max1 / max;
                double M = min1 / max;
                exp2.removeRange(m, M);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] <= 0</code>.
     */
    static public void productSetMaxN(double max, double min1, double max1, FloatExp exp2) throws Failure {
        if (max == 0) {
            if (max1 < 0) {
                exp2.setMin(0);
            }
        } else {
            double v = max > 0 ? max1 : min1;
            if (v != 0) {
                double min2 = max / v;
                exp2.setMin(min2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) <= max</code> where
     * <code>[min1..max1] >= 0</code>.
     */
    static public void productSetMaxP(double max, double min1, double max1, FloatExp exp2) throws Failure {
        if (max == 0) {
            if (min1 > 0) {
                exp2.setMax(0);
            }
        } else {
            double v = max > 0 ? min1 : max1;
            if (v != 0) {
                double max2 = max / v;
                exp2.setMax(max2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code>.
     */
    static public void productSetMin(double min, FloatExp exp1, FloatExp exp2) throws Failure {
        double min1, max1;
        // exp1 >= 0
        if ((min1 = exp1.min()) >= 0) {
            productSetMinP(min, min1, exp1.max(), exp2);
        }
        // exp1 <= 0
        else if ((max1 = exp1.max()) <= 0) {
            productSetMinN(min, min1, max1, exp2);
        } else // exp1 changes sign
        {
            if (min > 0) {
                double m = min1 / min;
                double M = max1 / min;
                exp2.removeRange(m, M);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] <= 0</code>.
     */
    static public void productSetMinN(double min, double min1, double max1, FloatExp exp2) throws Failure {
        if (min == 0) {
            if (max1 < 0) {
                exp2.setMax(0);
            }
        } else {
            double v = min > 0 ? min1 : max1;
            if (v != 0) {
                double max2 = min / v;
                exp2.setMax(max2);
            }
        }
    }

    /**
     * Adjust the expression exp2 so that: <code>min([min1..max1]*[min2..max2]) >= min</code> where
     * <code>[min1..max1] >= 0</code>.
     */
    static public void productSetMinP(double min, double min1, double max1, FloatExp exp2) throws Failure {
        if (min == 0) {
            if (min1 > 0) {
                exp2.setMin(0);
            }
        } else {
            double v = min > 0 ? max1 : min1;
            if (v != 0) {
                double min2 = min / v;
                exp2.setMin(min2);
            }
        }
    }

    /**
     * Returns x satisfying the equation: <code>y = pow(x,v)</code>.
     */
    public static double solve_pow(double y, double v) {
        // y=pow(x,v) -> pow(y,1/v)=pow(pow(x,v),1/v)=x
        double x = Math.pow(y, 1 / v);
        // System.out.println(y+"=pow("+x+","+v+")["+Math.pow(x,v)+"]"); //
        // check
        return x;
    }

    /**
     * Returns the expression: <code>max(sqr(min),sqr(max))</code>.
     */
    public static double sqrMax(double min, double max) {
        return Math.max(min * min, max * max);
    }

    /**
     * Returns the expression: <code>min(sqr(min),sqr(max))</code>.
     */
    public static double sqrMin(double min, double max) {
        // min >= 0 && max >= 0
        if (min >= 0) {
            return min * min;
        }

        // min < 0 && max >= 0
        if (max >= 0) {
            return 0;
        }

        // min < 0 && max < 0
        return max * max;
    }

} // ~ FloatCalc
