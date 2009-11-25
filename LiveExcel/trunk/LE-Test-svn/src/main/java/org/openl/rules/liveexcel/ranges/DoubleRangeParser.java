package org.openl.rules.liveexcel.ranges;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DoubleRangeParser {

    private double lowerBound = Double.NEGATIVE_INFINITY;
    private double upperBound = Double.POSITIVE_INFINITY;
    private BoundType lowerBoundType = BoundType.OPEN;
    private BoundType upperBoundType = BoundType.OPEN;

    // Supported range format: <min number> - <max number> or
    // [<, <=, >, >=]<number> or <number>+
    // Any symbols at the end are allowed to support expressions like
    // ">=2 km", "2+ M", "6-8 km^2"
    // format: <number>

    private static final String rangeJustNumberFormat = "^(\\$?[\\+\\-]?(\\d*\\.)?\\d+[KMBkmb]?).*";
    // format: <min number> - <max number>
    private static final String rangeMinMaxFormat = "(\\$?[\\+\\-]?(\\d*\\.)?\\d+[KMBkmb]?)\\-(\\$?[\\+\\-]?(\\d*\\.)?\\d+[KMBkmb]?).*";
    // format: [<, <=, >, >=]<number>
    private static final String rangePrefixFormat = "(\\<|\\<\\=|\\>|\\>\\=){1}(\\$?[\\+\\-]?(\\d*\\.)?\\d+[KMBkmb]?).*";
    // format: <number>[<, >, +]
    private static final String rangePostfixFormat = "(\\$?[\\+\\-]?(\\d*\\.)?\\d+[KMBkmb]?)(\\<|\\<\\=|\\>|\\>\\=|\\+){1}.*";

    private static Pattern justNumberFormatPattern = Pattern.compile(rangeJustNumberFormat);
    private static Pattern minMaxFormatPattern = Pattern.compile(rangeMinMaxFormat);
    private static Pattern prefixFormatPattern = Pattern.compile(rangePrefixFormat);
    private static Pattern postfixFormatPattern = Pattern.compile(rangePostfixFormat);

    /**
     * @param range Range to check
     * @return <code>true</code> if it is range.
     */
    public static boolean isRange(String range) {
        String rangeWithoutSpaces = range.replaceAll("\\s", "");
        Matcher justNumberFormatMatcher = justNumberFormatPattern.matcher(rangeWithoutSpaces);
        Matcher minMaxFormatMatcher = minMaxFormatPattern.matcher(rangeWithoutSpaces);
        Matcher prefixFormatMatcher = prefixFormatPattern.matcher(rangeWithoutSpaces);
        Matcher postfixFormatMatcher = postfixFormatPattern.matcher(rangeWithoutSpaces);

        if (!justNumberFormatMatcher.matches() && !minMaxFormatMatcher.matches() && !prefixFormatMatcher.matches()
                && !postfixFormatMatcher.matches()) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Parse string to range
     * 
     * @param range
     */
    public DoubleRange parse(String range) {
        String rangeWithoutSpaces = range.replaceAll("\\s", "");

        Matcher justNumberFormatMatcher = justNumberFormatPattern.matcher(rangeWithoutSpaces);
        Matcher minMaxFormatMatcher = minMaxFormatPattern.matcher(rangeWithoutSpaces);
        Matcher prefixFormatMatcher = prefixFormatPattern.matcher(rangeWithoutSpaces);
        Matcher postfixFormatMatcher = postfixFormatPattern.matcher(rangeWithoutSpaces);

        if (!isRange(range)) {
            throw new IllegalArgumentException(
                    "Range Format Error - format should be: <min number> - <max number> or [<, <=, >, >=]<number> or <number>+");
        }

        // Notice: more specific formats should be applied at the end
        // TODO: refactore to eliminate side effects and simultaneous processing
        // of several formats
        processJustNumber(justNumberFormatMatcher);
        processPostfix(postfixFormatMatcher);
        processMinMax(minMaxFormatMatcher);
        processPrefix(prefixFormatMatcher);
        return new DoubleRange(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    /**
     * Parse number with format: [$]<number>[K,M,B]
     * 
     * @param number
     * @return parsed number
     */
    public static double parseNumber(String number) {
        if (number.startsWith("$"))
            number = number.substring(1);
        char c = number.charAt(number.length() - 1);
        double multiplier = 1;
        switch (c) {
            case 'K':
            case 'k':
                multiplier = 1000;
                break;
            case 'M':
            case 'm':
                multiplier = 1000000;
                break;
            case 'B':
            case 'b':
                multiplier = 1000000000;
                break;
        }

        if (multiplier != 1)
            number = number.substring(0, number.length() - 1);

        return Double.parseDouble(number) * multiplier;

    }

    private void processJustNumber(Matcher justNumberFormatMatcher) {
        if (justNumberFormatMatcher.matches()) {
            lowerBound = parseNumber(justNumberFormatMatcher.group(1));
            upperBound = parseNumber(justNumberFormatMatcher.group(1));
            lowerBoundType = BoundType.CLOSED;
            upperBoundType = BoundType.CLOSED;

        }
    }

    /**
     * @param postfixFormatMatcher
     */
    private void processPostfix(Matcher postfixFormatMatcher) {
        if (postfixFormatMatcher.matches()) {
            double number = parseNumber(postfixFormatMatcher.group(1));
            String operation = postfixFormatMatcher.group(3);

            if (operation.equals(">") || operation.equals("+")) {
                lowerBound = number;
                lowerBoundType = BoundType.OPEN;

                upperBound = Double.POSITIVE_INFINITY;
                upperBoundType = BoundType.OPEN;
            }

            if (operation.equals("<")) {
                upperBound = number;
                upperBoundType = BoundType.OPEN;

                lowerBound = Double.NEGATIVE_INFINITY;
                lowerBoundType = BoundType.OPEN;
            }
        }
    }

    /**
     * @param prefixFormatMatcher
     */
    private void processPrefix(Matcher prefixFormatMatcher) {
        if (prefixFormatMatcher.matches()) {
            String operation = prefixFormatMatcher.group(1);
            double number = parseNumber(prefixFormatMatcher.group(2));

            if (operation.equals(">")) {
                lowerBound = number;
                lowerBoundType = BoundType.OPEN;

                upperBound = Double.POSITIVE_INFINITY;
                upperBoundType = BoundType.OPEN;
            }

            if (operation.equals(">=")) {
                lowerBound = number;
                lowerBoundType = BoundType.CLOSED;

                upperBound = Double.POSITIVE_INFINITY;
                upperBoundType = BoundType.OPEN;
            }

            if (operation.equals("<=")) {
                upperBound = number;
                upperBoundType = BoundType.CLOSED;

                lowerBound = Double.NEGATIVE_INFINITY;
                lowerBoundType = BoundType.OPEN;
            }

            if (operation.equals("<")) {
                upperBound = number;
                upperBoundType = BoundType.OPEN;

                lowerBound = Double.NEGATIVE_INFINITY;
                lowerBoundType = BoundType.OPEN;
            }
        }
    }

    /**
     * @param minMaxFormatMatcher
     */
    private void processMinMax(Matcher minMaxFormatMatcher) {
        if (minMaxFormatMatcher.matches()) {
            lowerBound = parseNumber(minMaxFormatMatcher.group(1));
            upperBound = parseNumber(minMaxFormatMatcher.group(3));
            lowerBoundType = BoundType.CLOSED;
            upperBoundType = BoundType.CLOSED;
        }
    }
}
