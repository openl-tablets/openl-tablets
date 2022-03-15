package org.openl.rules.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A set of functions to work with numbers
 */
public final class Numbers {

    public static final String POSITIVE_INFINITY_SIGN = "∞";
    public static final String NEGATIVE_INFINITY_SIGN = "-∞";
    private static final Pattern TRAILING_ZERO = Pattern.compile("(\\.0+$)|(?<=\\.\\d{0,20})0+$");

    private Numbers() {
        //Utility class
    }

    /**
     * Default {@code toString} method for numbers. Default format is {@code "#.###"}. See {@link #toString(Number, String)}
     * for details.
     *
     * @param number any instance of {@link Number}
     * @return string representation of a given number.
     */
    public static String toString(Number number) {
        if (number == null) {
            return null;
        }
        if (number instanceof Double) {
            Double doubleNumber = (Double) number;
            if (doubleNumber.isInfinite()) {
                return doubleNumber > 0 ? POSITIVE_INFINITY_SIGN : NEGATIVE_INFINITY_SIGN;
            } 
        } else if (number instanceof Float) {
            Float floatNumber = (Float) number;
            if (floatNumber.isInfinite()) {
                return floatNumber > 0 ? POSITIVE_INFINITY_SIGN : NEGATIVE_INFINITY_SIGN;
            }
        }
        if (number instanceof Double || number instanceof Float || number instanceof BigDecimal) {
            return TRAILING_ZERO.matcher(number.toString()).replaceAll(""); // remove zeros
        } else {
            return number.toString();
        }
    }

    /**
     * Formatting method for numbers. Uses {@code pattern} parameter for defining output format.
     * Format symbols:
     * <table>
     *     <thead>
     *          <tr><th>Symbol&nbsp;</th><th>Location&nbsp;</th><th>Symbol&nbsp;</th><th>Meaning&nbsp;</th></tr>
     *     </thead>
     *     <tbody>
     *          <tr><td>0</td><td>Number</td><td></td><td>Digit</td></tr>
     *          <tr><td>#</td><td>Number</td><td></td><td>Digit, zero shows as absent</td></tr>
     *          <tr><td>.</td><td>Number</td><td>.</td><td>Decimal separator or monetary decimal separator</td></tr>
     *          <tr><td>-</td><td>Number</td><td>-</td><td>Minus sign</td></tr>
     *          <tr><td>,</td><td>Number</td><td>,</td><td>Grouping separator</td></tr>
     *          <tr><td>E</td><td>Number</td><td>E</td><td>Separates mantissa and exponent in scientific notation. Need not be quoted in prefix or suffix.</td></tr>
     *          <tr><td>;</td><td>Subpattern boundary</td><td></td><td>Separates positive and negative subpatterns</td></tr>
     *          <tr><td>%</td><td>Prefix or suffix</td><td>%</td><td>Multiply by 100 and show as percentage</td></tr>
     *          <tr><td>‰</td><td>Prefix or suffix</td><td>‰</td><td>Multiply by 1000 and show as per mille value</td></tr>
     *          <tr><td>'</td><td>Prefix or suffix</td><td></td><td>Used to quote special characters in a prefix or suffix, for example, "'#'#" formats 123 to "#123". To create a single quote itself, use two in a row: "# o''clock".</td></tr>
     *     </tbody>
     * </table>
     * <p/>
     * Some examples:
     * <table border="1">
     *     <thead>
     *          <tr></th><th>Pattern&nbsp;</th><th>Number&nbsp;</th><th>Result&nbsp;</th><th>Comment&nbsp;</th></tr>
     *     </thead>
     *     <tbody>
     *         <tr><td>0000&nbsp;</td><td>42&nbsp;</td><td>0042&nbsp;</td></tr>
     *         <tr><td>0000.000&nbsp;</td><td>42.42&nbsp;</td><td>0042.420&nbsp;</td></tr>
     *         <tr><td>0000.000&nbsp;</td><td>42&nbsp;</td><td>0042.000&nbsp;</td></tr>
     *         <tr><td>#.##&nbsp;</td><td>3000.452&nbsp;</td><td>3000.45&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>#.##&nbsp;</td><td>3000.4&nbsp;</td><td>3000.4&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>#.##&nbsp;</td><td>3000&nbsp;</td><td>3000&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>#,###.#&nbsp;</td><td>12000&nbsp;</td>12,000&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>#0E0&nbsp;</td><td>12000&nbsp;</td>1.2E4&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>003&nbsp;</td><td>&nbsp;</td>&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>000;neg 0&nbsp;</td><td>3&nbsp;</td>003&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>000;neg 0&nbsp;</td><td>-3&nbsp;</td>neg 003&nbsp;</td><td>For negative numbers positive pattern enhanced by negative pattern is used&nbsp;</td></tr>
     *         <tr><td>0%&nbsp;</td><td>0.03&nbsp;</td>3%&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>0‰&nbsp;</td><td>0.03&nbsp;</td>30‰&nbsp;</td><td>&nbsp;</td></tr>
     *         <tr><td>'#'0'%'&nbsp;</td><td>3&nbsp;</td>#3%&nbsp;</td><td>&nbsp;</td></tr>
     *     </tbody>
     * </table>
     *
     *
     * @param number any instance of {@link Number}
     * @param pattern target pattern
     * @return string representation of a given number.
     */
    public static String toString(Number number, String pattern) {
        if (number == null || pattern == null) {
            return null;
        }
        try {
            NumberFormat decimalFormat = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
            return decimalFormat.format(number);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
