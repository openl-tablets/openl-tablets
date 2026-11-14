package org.openl.rules.testmethod.result;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * How far a returned number may be from the expected one in a test, as the precision of a test table or of its
 * column sets it.
 * <p>
 * A precision {@code N}, a whole number, allows the difference {@code 1E-N}: {@code 5} allows {@code 0.00001}, and
 * {@code -2} allows {@code 100}.
 * <p>
 * A precision in significant figures rounds both values half up at the last significant figure of the expected value,
 * and they must be equal. {@code Nsf}, such as {@code 4sf}, counts {@code N} figures: {@code 99.00} matches
 * {@code 98.995}, but not {@code 99.005}, which rounds to {@code 99.01}. {@code sf} takes the figures the expected
 * value is written with: {@code 10.00} matches {@code 10.004}, {@code 10.000} does not, and {@code 0.0001} matches
 * {@code 0.00006}, but not {@code 0.00004}.
 *
 * @author Yury Molchan
 */
public sealed interface Tolerance {

    /**
     * The regular expression of a precision text.
     */
    String PRECISION_REGEX = "-?\\d+|([1-9]\\d*)?sf";

    /**
     * The compiled {@link #PRECISION_REGEX}.
     */
    Pattern PRECISION_PATTERN = Pattern.compile(PRECISION_REGEX);

    /**
     * A difference of at most {@code value}.
     */
    record Delta(BigDecimal value) implements Tolerance {
    }

    /**
     * An agreement to significant figures of the expected value.
     */
    sealed interface Figures extends Tolerance {
    }

    /**
     * An agreement to {@code count} significant figures of the expected value.
     */
    record SignificantFigures(int count) implements Figures {
    }

    /**
     * An agreement to the significant figures the expected value is written with, down to the decimal place
     * {@code scale}: 2 for {@code 10.00}. Without a scale, the expected value is taken as it reads.
     */
    record ExpectedFigures(@Nullable Integer scale) implements Figures {
    }

    /**
     * Reads the precision of a test.
     *
     * @param precision a whole number of decimal places, such as {@code 5} or {@code -2}, a positive number of
     *                  significant figures, such as {@code 4sf}, or {@code sf} for the figures of the expected value
     * @return the tolerance, or {@code null} for a text that is not a precision
     */
    static @Nullable Tolerance of(String precision) {
        if (!PRECISION_PATTERN.matcher(precision).matches()) {
            return null;
        }
        try {
            if ("sf".equals(precision)) {
                return new ExpectedFigures(null);
            } else if (precision.endsWith("sf")) {
                return new SignificantFigures(Integer.parseInt(precision, 0, precision.length() - 2, 10));
            }
            return new Delta(new BigDecimal(BigInteger.ONE, Integer.parseInt(precision)));
        } catch (NumberFormatException e) {
            // a number beyond the Integer range
            return null;
        }
    }
}
