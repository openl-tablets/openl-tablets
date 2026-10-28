package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * The values doubles count as in the decimal arithmetic, computed with {@link BigDecimal}: the reference the fast
 * arithmetic is tested against.
 */
final class DecimalModel {

    private static final MathContext SHOWN_DIGITS = new MathContext(15, RoundingMode.HALF_EVEN);

    private DecimalModel() {
        // Utility class
    }

    /**
     * The value a double counts as: itself when it is a computed number, its shortest decimal otherwise.
     */
    static BigDecimal value(double x) {
        return isComputed(x) ? Decimal.exactValue(x) : BigDecimal.valueOf(x);
    }

    /**
     * A number with a fraction whose shortest decimal has more than 15 digits.
     */
    static boolean isComputed(double x) {
        return Double.isFinite(x) && Math.rint(x) != x && BigDecimal.valueOf(x).stripTrailingZeros().precision() > 15;
    }

    /**
     * The rounding a computed number carries: twice its last place.
     */
    static double rounding(double x) {
        return isComputed(x) ? 2 * Math.ulp(x) : 0.0;
    }

    /**
     * The decimal of at most 13 digits that {@code r} shows rounded to 15, when it lies within the {@code rounding}.
     */
    static double snap(double r, double rounding) {
        var abs = Math.abs(r);
        if (rounding == 0 || !(abs >= 0x1p-900 && abs <= 0x1p900)) {
            return r;
        }
        var shown = BigDecimal.valueOf(r).round(SHOWN_DIGITS).stripTrailingZeros();
        var near = shown.precision() <= 13 && Decimal.exactValue(r).subtract(shown).abs().doubleValue() <= rounding;
        return near ? shown.doubleValue() : r;
    }
}
