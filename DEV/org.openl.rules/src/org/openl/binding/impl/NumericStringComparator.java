package org.openl.binding.impl;

import java.util.Comparator;

/**
 * Compares Strings with numbers in the math order of numbers.
 *
 * <pre>
 *     "01"
 *     "2"
 *     "03A"
 *     "99BBBBB"
 *     "500"
 *     "ABC-001"
 *     "ABC-9"
 *     "ABC-10"
 *     "Abc-0"
 *
 * </pre>
 *
 * @author Yury Molchan
 */
public class NumericStringComparator implements Comparator<CharSequence> {

    public static final NumericStringComparator INSTANCE = new NumericStringComparator();

    @Override
    public int compare(CharSequence str1, CharSequence str2) {
        final var length1 = str1.length();
        final var length2 = str2.length();
        var i1 = 0;
        var i2 = 0;
        while (i1 < length1 && i2 < length2) {
            var ch1 = str1.charAt(i1);
            var ch2 = str2.charAt(i2);
            if (!Character.isDigit(ch1) || !Character.isDigit(ch2)) {
                // Usual String.compareTo() logic.
                if (ch1 != ch2) {
                    return ch1 - ch2;
                }
                i1++;
                i2++;
                continue;
            }

            // Searching begin of the number to compare
            i1 = skipInsignificantZeros(str1, i1);
            i2 = skipInsignificantZeros(str2, i2);

            // Searching end of the number to compare
            var exp1 = countDigits(str1, i1);
            i1 += exp1;
            var exp2 = countDigits(str2, i2);
            i2 += exp2;

            if (exp1 != exp2) {
                // the first number greater than the second by the exponent
                return Integer.compare(exp1, exp2);
            }

            var result = compareDigits(str1, i1, str2, i2, exp1);
            if (result != 0) {
                return result;
            }
        }

        if (i1 == length1 && i2 == length2) {
            // comparison has not found difference at the end of the both strings.
            return 0;
        }

        return length1 - length2;

    }

    /**
     * Skips the insignificant zeros of the number that starts at the given index.
     *
     * @return the index of the first significant digit, or of the last digit when the number has only zeros
     */
    private static int skipInsignificantZeros(CharSequence str, int start) {
        final var length = str.length();
        var i = start;
        var ch = str.charAt(i);
        while (isZero(ch) && i < length - 1 && Character.isDigit(str.charAt(i + 1))) {
            // Skip insignificant zero.
            i++;
            ch = str.charAt(i);
        }
        return i;
    }

    /**
     * Counts the digits of the number that starts at the given index.
     */
    private static int countDigits(CharSequence str, int start) {
        final var length = str.length();
        var i = start;
        var ch = str.charAt(i);
        var exp = 0;
        while (Character.isDigit(ch)) {
            exp++;
            i++;
            if (i >= length) {
                break;
            }
            ch = str.charAt(i);
        }
        return exp;
    }

    /**
     * Compares two numbers with the same count of digits, which end before the given indexes.
     */
    private static int compareDigits(CharSequence str1, int end1, CharSequence str2, int end2, int digits) {
        for (var exp = digits; exp > 0; exp--) {
            // compare numbers starting from the most significant digits of a number
            var dig1 = Character.digit(str1.charAt(end1 - exp), 10);
            var dig2 = Character.digit(str2.charAt(end2 - exp), 10);
            if (dig1 != dig2) {
                return Integer.compare(dig1, dig2);
            }
        }
        return 0;
    }

    /**
     * Calculates a hash according to the same algorithm of {@linkplain #compare} method. So If {@linkplain #compare}
     * returns zero then both string have the same hash.
     */
    public static int hashCode(CharSequence value) {
        final var prime = 31;
        var result = 1;
        var length = value.length();
        var leading = true;
        for (var i = 0; i < length; i++) {
            var c = value.charAt(i);
            if (leading && isZero(c)) {
                // don't calculate hash for insignificant zero.
                continue;
            }
            leading = !Character.isDigit(c);
            result = prime * result + c;
        }
        return result;
    }

    /**
     * Checks if a character is a zero according to the Unicode codepoint.
     */
    private static boolean isZero(char ch2) {
        return Character.digit(ch2, 10) == 0;
    }
}
