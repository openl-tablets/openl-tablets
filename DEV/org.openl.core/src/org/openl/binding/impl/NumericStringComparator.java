package org.openl.binding.impl;

import java.util.Comparator;

/**
 * Compares Strings with numbers in the math order of numbers.
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
        final int length1 = str1.length();
        final int length2 = str2.length();
        int i1 = 0;
        int i2 = 0;
        while (i1 < length1 && i2 < length2) {
            char ch1 = str1.charAt(i1);
            char ch2 = str2.charAt(i2);
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
            while (isZero(ch1) && i1 < length1 - 1) {
                // Skip insignificant zero.
                i1++;
                ch1 = str1.charAt(i1);
            }

            while (isZero(ch2) && i2 < length2 - 1) {
                // Skip insignificant zero.
                i2++;
                ch2 = str2.charAt(i2);
            }

            // Searching end of the number to compare
            int exp1 = 0;
            while (Character.isDigit(ch1)) {
                exp1++;
                i1++;
                if (i1 >= length1) {
                    break;
                }
                ch1 = str1.charAt(i1);
            }

            int exp2 = 0;
            while (Character.isDigit(ch2)) {
                exp2++;
                i2++;
                if (i2 >= length2) {
                    break;
                }
                ch2 = str2.charAt(i2);
            }

            if (exp1 != exp2) {
                // the first number greater than the second by the exponent
                return exp1 - exp2;
            }

            for (; exp1 > 0; exp1--) {
                // compare numbers starting from the most significant digits of a number
                int dig1 = Character.digit(str1.charAt(i1 - exp1), 10);
                int dig2 = Character.digit(str2.charAt(i2 - exp1), 10);
                if (dig1 != dig2) {
                    return dig1 - dig2;
                }
            }
        }

        if (i1 == length1 && i2 == length2) {
            // comparison has not found difference at the end of the both strings.
            return 0;
        }

        return length1 - length2;

    }

    /**
     * Calculates a hash according to the same algorithm of {@linkplain #compare} method.
     * So If {@linkplain #compare} returns zero then both string have the same hash.
     */
    public static int hashCode(CharSequence value) {
        final int prime = 31;
        int result = 1;
        int length = value.length();
        boolean leading = true;
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
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
