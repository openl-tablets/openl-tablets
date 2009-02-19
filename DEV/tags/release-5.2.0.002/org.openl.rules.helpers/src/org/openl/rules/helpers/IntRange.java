/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.domain.IntRangeDomain;

/**
 * Integer range.
 * @author snshor
 */
public class IntRange extends IntRangeDomain implements INumberRange {

    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with variety of formats.
     * Supported range formats: "<min number> - <max number>" or "[<, <=, >, >=]<number>" or "<number>+"
     * Also numbers can be enhanced with $ sign and K,M,B, e.g. $1K = 1000 
     * Any symbols at the end are allowed to support expressions like ">=2 barrels", "6-8 km^2"  
     * @param range
     */
    public IntRange(String range) {
        super(0, 0);
        new Parser().parse(range);
    }

    /**
     * Constructor for <code>IntRange</code> with provided <code>min</code> and <code>max</code> values.
     * @param min
     * @param max
     */
    public IntRange(int min, int max) {
        super(min, max);
    }

    /**
     * Parse number with format: [$]<number>[K,M,B]
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
    
    private enum BoundType {
        CLOSED(0), OPEN(1);

        private final int rangeAdjustment;

        BoundType(int rangeAdjustment) {
            this.rangeAdjustment = rangeAdjustment;
        }

        public int getRangeAdjustment() {
            return rangeAdjustment;
        }
    }

    private class Parser {
        private int lowerBound = Integer.MIN_VALUE;
        private int upperBound = Integer.MAX_VALUE;
        private BoundType lowerBoundType = BoundType.CLOSED;
        private BoundType upperBoundType = BoundType.CLOSED;

        // Supported range format: <min number> - <max number> or 
        // [<, <=, >, >=]<number> or <number>+
        // Any symbols at the end are allowed to support expressions like 
        // ">=2 km", "2+ M", "6-8 km^2"
        // format: <number>
        private final String rangeJustNumberFormat = "^(\\$?[\\+|\\-]?[0-9]+[KMBkmb]?).*";
        // format: <min number> - <max number>
        private final String rangeMinMaxFormat = "(\\$?[\\+|\\-]?[0-9]+[KMBkmb]?)\\-(\\$?[\\+|\\-]?[0-9]+[KMBkmb]?).*";
        // format: [<, <=, >, >=]<number>
        private final String rangeMoreLessFormat = "(\\<|\\<\\=|\\>|\\>\\=){1}(\\$?[\\+|\\-]?[0-9]+[KMBkmb]?).*";
        // format: <number>+
        private final String rangePlusFormat = "(\\$?[\\+|\\-]?[0-9]+[KMBkmb]?)\\+.*";

        private Pattern justNumberFormatPattern = Pattern.compile(rangeJustNumberFormat);
        private Pattern minMaxFormatPattern = Pattern.compile(rangeMinMaxFormat);
        private Pattern moreLessFormatPattern = Pattern.compile(rangeMoreLessFormat);
        private Pattern plusFormatPattern = Pattern.compile(rangePlusFormat);

        /**
         * Parse string to range
         * 
         * @param range
         */
        public void parse(String range) {
            String rangeWithoutSpaces = range.replaceAll("\\s", "");

            Matcher justNumberFormatMatcher = justNumberFormatPattern.matcher(rangeWithoutSpaces);
            Matcher minMaxFormatMatcher = minMaxFormatPattern.matcher(rangeWithoutSpaces);
            Matcher moreLessFormatMatcher = moreLessFormatPattern.matcher(rangeWithoutSpaces);
            Matcher plusFormatMatcher = plusFormatPattern.matcher(rangeWithoutSpaces);

            if (!justNumberFormatMatcher.matches() && !minMaxFormatMatcher.matches() && !moreLessFormatMatcher.matches() && !plusFormatMatcher.matches()) {
                throw new IllegalArgumentException(
                        "Range Format Error - format should be: <min number> - <max number> or [<, <=, >, >=]<number> or <number>+");
            }

            // Notice: more specific formats should be applied at the end
            // TODO: refactore to eliminate side effects and simultaneous processing of several formats
            processJustNumber(justNumberFormatMatcher);
            processPlus(plusFormatMatcher);
            processMinMax(minMaxFormatMatcher);
            processMoreLess(moreLessFormatMatcher);

            applyParsedValues();
        }

        private void processJustNumber(Matcher justNumberFormatMatcher) {
            if (justNumberFormatMatcher.matches()) {
                lowerBound = (int) parseNumber(justNumberFormatMatcher.group(1));
                upperBound = (int) parseNumber(justNumberFormatMatcher.group(1));
                lowerBoundType = BoundType.CLOSED;
                upperBoundType = BoundType.CLOSED;

            }
        }

        /**
         * @param plusFormatMatcher
         */
        private void processPlus(Matcher plusFormatMatcher) {
             if (plusFormatMatcher.matches()) {
                lowerBound = (int) parseNumber(plusFormatMatcher.group(1));
                lowerBoundType = BoundType.CLOSED;
                
                upperBound = Integer.MAX_VALUE;
                upperBoundType = BoundType.CLOSED;
            }
        }

        /**
         * @param moreLessFormatMatcher
         */
        private void processMoreLess(Matcher moreLessFormatMatcher) {
            if (moreLessFormatMatcher.matches()) {
                String operation = moreLessFormatMatcher.group(1);
                int number = (int) parseNumber(moreLessFormatMatcher.group(2));

                if (operation.equals(">")) {
                    lowerBound = number;
                    lowerBoundType = BoundType.OPEN;
                    
                    upperBound = Integer.MAX_VALUE;
                    upperBoundType = BoundType.CLOSED;
                }

                if (operation.equals(">=")) {
                    lowerBound = number;
                    lowerBoundType = BoundType.CLOSED;
                    
                    upperBound = Integer.MAX_VALUE;
                    upperBoundType = BoundType.CLOSED;
                }

                if (operation.equals("<=")) {
                    upperBound = number;
                    upperBoundType = BoundType.CLOSED;
                    
                    lowerBound = Integer.MIN_VALUE;
                    lowerBoundType = BoundType.CLOSED;
                }

                if (operation.equals("<")) {
                    upperBound = number;
                    upperBoundType = BoundType.OPEN;
                    
                    lowerBound = Integer.MIN_VALUE;
                    lowerBoundType = BoundType.CLOSED;
                }
            }
        }

        /**
         * @param minMaxFormatMatcher
         */
        private void processMinMax(Matcher minMaxFormatMatcher) {
            if (minMaxFormatMatcher.matches()) {
                lowerBound = (int) parseNumber(minMaxFormatMatcher.group(1));
                upperBound = (int) parseNumber(minMaxFormatMatcher.group(2));
                lowerBoundType = BoundType.CLOSED;
                upperBoundType = BoundType.CLOSED;
            }
        }

        /**
         * Applies parsed values to range instance
         */
        private void applyParsedValues() {
            min = lowerBound + lowerBoundType.getRangeAdjustment();
            max = upperBound - upperBoundType.getRangeAdjustment();
        }
    }

}
