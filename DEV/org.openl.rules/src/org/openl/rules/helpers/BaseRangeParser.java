package org.openl.rules.helpers;

import org.openl.util.RangeWithBounds;
import org.openl.util.StringUtils;

public abstract class BaseRangeParser implements RangeParser {
    protected String minNumber = "";
    protected String minMultiplier = "";
    protected String maxNumber = "";
    protected String maxMultiplier = "";

    public String getMinNumber() {
        return minNumber;
    }

    public String getMaxNumber() {
        return maxNumber;
    }

    public String getMinMultiplier() {
        return minMultiplier;
    }

    public String getMaxMultiplier() {
        return maxMultiplier;
    }

    protected long parseMultiplier(String suffix) {
        long multiplier;
        if ("K".equals(suffix)) {
            multiplier = 1000;
        } else if ("M".equals(suffix)) {
            multiplier = 1000000;
        } else if ("B".equals(suffix)) {
            multiplier = 1000000000;
        } else {
            throw new IllegalArgumentException(String.format("Suffix %s is not supported in ranges", suffix));
        }
        return multiplier;
    }

    protected long parseIntWithMultiplier(String number, String suffix) {
        long result = Long.parseLong(number.replace(",", ""));

        if (StringUtils.isNotEmpty(suffix)) {
            result *= parseMultiplier(suffix);
        }

        return result;
    }

    protected double parseDoubleWithMultiplier(String number, String suffix) {
        double result = Double.parseDouble(number.replace(",", ""));

        if (StringUtils.isNotEmpty(suffix)) {
            result *= parseMultiplier(suffix);
        }

        return result;
    }

    protected Number getMax(Number number) {
        if (number.getClass() == Double.class) {
            return Double.POSITIVE_INFINITY;
        } else if (number.getClass() == Long.class) {
            return Long.MAX_VALUE;
        } else {
            return Integer.MAX_VALUE;
        }
    }

    protected Number getMin(Number number) {
        if (number.getClass() == Double.class) {
            return Double.NEGATIVE_INFINITY;
        } else if (number.getClass() == Long.class) {
            return Long.MIN_VALUE;
        } else {
            return Integer.MIN_VALUE;
        }
    }

    /**
     * Determine, which number is min, which is max and what bounds are.
     *
     * @param first first number in the parsing sequence
     * @param second second number in the parsing sequence
     * @param firstBound one of "&lt;", "&lt;=", "&gt;", "&gt;="
     * @param secondBound one of "&lt;", "&lt;=", "&gt;", "&gt;="
     * @return Parsed range
     */
    protected RangeWithBounds getRangeWithBounds(Number first, Number second, String firstBound, String secondBound) {
        Number min;
        Number max;
        RangeWithBounds.BoundType minBound;
        RangeWithBounds.BoundType maxBound;

        if (firstBound.startsWith("<")) {
            if (secondBound.startsWith("<")) {
                return null;
            }
            min = second;
            max = first;
            minBound = secondBound.equals(">") ? RangeWithBounds.BoundType.EXCLUDING
                                               : RangeWithBounds.BoundType.INCLUDING;
            maxBound = firstBound.equals("<") ? RangeWithBounds.BoundType.EXCLUDING
                                              : RangeWithBounds.BoundType.INCLUDING;

            String t;
            t = minNumber;
            minNumber = maxNumber;
            maxNumber = t;

            t = minMultiplier;
            minMultiplier = maxMultiplier;
            maxMultiplier = t;
        } else {
            if (secondBound.startsWith(">")) {
                return null;
            }
            min = first;
            max = second;
            minBound = firstBound.equals(">") ? RangeWithBounds.BoundType.EXCLUDING
                                              : RangeWithBounds.BoundType.INCLUDING;
            maxBound = secondBound.equals("<") ? RangeWithBounds.BoundType.EXCLUDING
                                               : RangeWithBounds.BoundType.INCLUDING;
        }

        return new RangeWithBounds(min, max, minBound, maxBound);
    }
}
