package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.util.regex.Pattern;

/**
 * A number bound of an {@link IntRange} or a {@link DoubleRange}, written the way business users write amounts.
 *
 * <p>A bound may start with a {@code $} sign, may group the thousands of its whole part with commas and may end with
 * a {@code K}, {@code M} or {@code B} multiplier. So {@code $1,500}, {@code 2.5K} and {@code 1M} are 1500, 2500 and
 * 1000000.
 *
 * <p>A comma must separate groups of exactly three digits, so {@code 77,99} and {@code 1,0000} are not numbers.
 * Formulas do not accept these forms: there a comma separates values and {@code $} starts a step reference.
 *
 * @param number     the number without the {@code $} sign, the commas and the multiplier
 * @param multiplier 1, or the value of the multiplier
 * @author Yury Molchan
 */
record NumberBound(String number, long multiplier) {

    // Linear: possessive quantifiers never backtrack.
    @SuppressWarnings("java:S8786")
    private static final Pattern GROUPED = Pattern.compile("[-+]?+\\d{1,3}+(?:,\\d{3}+)++(?:\\.\\d++)?+");

    /**
     * Splits a bound into its number and its multiplier.
     *
     * @throws NumberFormatException when a comma does not separate groups of three digits
     */
    static NumberBound of(String text) {
        var start = text.startsWith("$") ? 1 : 0;
        var end = text.length();
        var multiplier = end > start ? multiplier(text.charAt(end - 1)) : 1L;
        if (multiplier > 1) {
            end--;
        }
        var number = text.substring(start, end);
        if (number.indexOf(',') >= 0) {
            if (!GROUPED.matcher(number).matches()) {
                throw new NumberFormatException("For input string: \"" + text + "\"");
            }
            number = number.replace(",", "");
        }
        return new NumberBound(number, multiplier);
    }

    private static long multiplier(char suffix) {
        return switch (suffix) {
            case 'K' -> 1_000L;
            case 'M' -> 1_000_000L;
            case 'B' -> 1_000_000_000L;
            default -> 1L;
        };
    }

    /**
     * Returns the value of a whole number bound.
     *
     * @throws NumberFormatException when the number is not a whole number
     * @throws ArithmeticException   when the value overflows a {@code long}
     */
    long toLong() {
        return Math.multiplyExact(Long.parseLong(number), multiplier);
    }

    /**
     * Returns the value of a bound. A multiplied value is rounded once, so {@code 1.005K} is exactly 1005.
     *
     * @throws NumberFormatException when the number ends with a dot or a type letter or has an exponent, as a Java
     *                               number may
     */
    double toDouble() {
        if (number.isEmpty() || !Character.isDigit(number.charAt(number.length() - 1)) || number.indexOf('e') >= 0
                || number.indexOf('E') >= 0) {
            throw new NumberFormatException("For input string: \"" + number + "\"");
        }
        if (multiplier == 1) {
            return Double.parseDouble(number);
        }
        return new BigDecimal(number).multiply(BigDecimal.valueOf(multiplier)).doubleValue();
    }
}
