package org.openl.rules.convertor;

import java.util.regex.Pattern;

/**
 * The ways business users write numbers, shared by formulas, cells and ranges.
 *
 * @author Yury Molchan
 */
public final class NumberNotation {

    // Linear: possessive quantifiers never backtrack.
    @SuppressWarnings("java:S8786")
    private static final Pattern GROUPED = Pattern.compile("[-+]?+[1-9]\\d{0,2}+(?:,\\d{3}+)++(?!\\d)");

    private NumberNotation() {
    }

    /**
     * Removes the commas that group the thousands of a number, as in {@code 1,000.00} or {@code 12,345,678}.
     *
     * <p>A comma groups thousands only in the whole part of a number, between groups of exactly three digits, and when
     * the first group does not start with zero. A text with any other comma, such as {@code 12,34.00},
     * {@code 1,00,000}, {@code 100.000,001} or {@code 0,500}, is returned as is, so a number parser rejects it.
     */
    public static String removeThousandsSeparators(String number) {
        if (number.indexOf(',') < 0) {
            return number;
        }
        var matcher = GROUPED.matcher(number);
        if (!matcher.lookingAt() || number.indexOf(',', matcher.end()) >= 0) {
            return number;
        }
        return number.replace(",", "");
    }

    /**
     * Returns the value of a multiplier suffix: 1000 for {@code K}, 1000000 for {@code M} and 1000000000 for
     * {@code B}. Any other character, also a small letter, is not a multiplier, and its value is 1.
     */
    public static long multiplier(char suffix) {
        return switch (suffix) {
            case 'K' -> 1_000L;
            case 'M' -> 1_000_000L;
            case 'B' -> 1_000_000_000L;
            default -> 1L;
        };
    }
}
