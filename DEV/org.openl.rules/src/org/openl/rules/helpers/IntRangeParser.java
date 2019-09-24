package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.exception.OpenLRuntimeException;
import org.openl.util.RangeWithBounds;
import org.openl.util.StringUtils;

public class IntRangeParser {
    private static final IntRangeParser INSTANCE = new IntRangeParser();

    private static final String INT_PATTERN = "\\$?(-?(?:\\d{1,19},){0,19}\\d{1,19})([KMB]?)";

    protected final RangeParser[] PARSERS = { new SimpleRangeParser(),
            new RangeWithBracketsParser(),
            new PrefixRangeParser(),
            new SuffixRangeParser(),
            new NumberParser(),
            new RangeWithMoreLessSymbolsParser(),
            new VerboseRangeParser() };

    protected IntRangeParser() {
    }

    public static IntRangeParser getInstance() {
        return INSTANCE;
    }

    private static final int MAX_RANGE_POSSIBLE_LENGTH = 100;

    public RangeWithBounds parse(String range) {
        if (range != null && range.length() <= MAX_RANGE_POSSIBLE_LENGTH) {
            try {
                range = range.trim();
                for (RangeParser parser : PARSERS) {
                    RangeWithBounds value;

                    value = parser.parse(range);
                    if (value != null) {
                        return value;
                    }
                }
            } catch (RuntimeException e) {
                throw new OpenLRuntimeException("Failed to parse int range.", e);
            }
        }
        throw new OpenLRuntimeException("Failed to parse int range.");
    }

    private static final class NumberParser extends BaseRangeParser {
        // Just a simple number like "$-55,000K" (minus 55000 thousands)
        private static final Pattern PATTERN = Pattern.compile(INT_PATTERN);

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String number = matcher.group(1);
            String multiplier = matcher.group(2);
            minNumber = maxNumber = number;
            minMultiplier = maxMultiplier = multiplier;

            long value = parseIntWithMultiplier(number, multiplier);

            return new RangeWithBounds(value, value);
        }
    }

    private static final class PrefixRangeParser extends BaseRangeParser {
        // <= 123M
        private static final Pattern PATTERN = Pattern.compile("(<=?|>=?|less\\s+than|more\\s+than)\\s*" + INT_PATTERN);
        private static final Pattern LESS_THAN_PATTERN = Pattern.compile("less\\s+than");
        private static final Pattern MORE_THAN_PATTERN = Pattern.compile("more\\s+than");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String prefix = matcher.group(1);
            String number = matcher.group(2);
            String multiplier = matcher.group(3);
            long value = parseIntWithMultiplier(number, multiplier);

            if ("<".equals(prefix) || LESS_THAN_PATTERN.matcher(prefix).matches()) {
                maxNumber = number;
                maxMultiplier = multiplier;
                return new RangeWithBounds(getMin(value),
                    value,
                    RangeWithBounds.BoundType.INCLUDING,
                    RangeWithBounds.BoundType.EXCLUDING);
            } else if ("<=".equals(prefix)) {
                maxNumber = number;
                maxMultiplier = multiplier;
                return new RangeWithBounds(getMin(value),
                    value,
                    RangeWithBounds.BoundType.INCLUDING,
                    RangeWithBounds.BoundType.INCLUDING);
            } else if (">".equals(prefix) || MORE_THAN_PATTERN.matcher(prefix).matches()) {
                minNumber = number;
                minMultiplier = multiplier;
                return new RangeWithBounds(value,
                    getMax(value),
                    RangeWithBounds.BoundType.EXCLUDING,
                    RangeWithBounds.BoundType.INCLUDING);
            } else if (">=".equals(prefix)) {
                minNumber = number;
                minMultiplier = multiplier;
                return new RangeWithBounds(value,
                    getMax(value),
                    RangeWithBounds.BoundType.INCLUDING,
                    RangeWithBounds.BoundType.INCLUDING);
            }

            // Shouldn't occur if regular expression is correct
            throw new IllegalArgumentException("Incorrect prefix");
        }
    }

    private static final class SuffixRangeParser extends BaseRangeParser {
        // 34+
        private static final Pattern PATTERN = Pattern.compile(INT_PATTERN + "\\s*(\\+|and\\s+more|or\\s+less)");
        private static final Pattern OR_LESS_PATTERN = Pattern.compile("or\\s+less");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String number = matcher.group(1);
            String multiplier = matcher.group(2);
            long value = parseIntWithMultiplier(number, multiplier);

            String suffix = matcher.group(3);
            if (OR_LESS_PATTERN.matcher(suffix).matches()) {
                maxNumber = number;
                maxMultiplier = multiplier;
                return new RangeWithBounds(getMin(value),
                    value,
                    RangeWithBounds.BoundType.INCLUDING,
                    RangeWithBounds.BoundType.INCLUDING);
            } else {
                minNumber = number;
                minMultiplier = multiplier;
                return new RangeWithBounds(value,
                    getMax(value),
                    RangeWithBounds.BoundType.INCLUDING,
                    RangeWithBounds.BoundType.INCLUDING);
            }
        }
    }

    private static final class SimpleRangeParser extends BaseRangeParser {
        // 34 - 123
        private static final Pattern PATTERN = Pattern
            .compile(INT_PATTERN + "\\s*([-;…]|\\.{3}|\\.{2}?)\\s*" + INT_PATTERN);

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }
            // TODO need to be refactored. Local variables has been created for thread safe, but they still need for
            // TableEdiorController
            String group1 = matcher.group(1);
            String group2 = matcher.group(2);
            String group4 = matcher.group(4);
            String group5 = matcher.group(5);
            String separator = matcher.group(3);
            minNumber = group1;
            maxNumber = group4;
            minMultiplier = group2;
            maxMultiplier = group5;

            long min = parseIntWithMultiplier(group1, group2);
            long max = parseIntWithMultiplier(group4, group5);

            RangeWithBounds.BoundType boundType = "…".equals(separator) || "..."
                .equals(separator) ? RangeWithBounds.BoundType.EXCLUDING : RangeWithBounds.BoundType.INCLUDING;

            return new RangeWithBounds(min, max, boundType, boundType);
        }
    }

    private static final class RangeWithBracketsParser extends BaseRangeParser {
        // [34 - 123)
        private static final Pattern PATTERN = Pattern.compile(
            "([\\[\\(])\\s*" + INT_PATTERN + "\\s*([-;…]|\\.{3}|\\.{2}?)\\s*" + INT_PATTERN + "\\s*([\\]\\)])");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }
            // TODO need to be refactored. Local variables has been created for thread safe, but they still need for
            // TableEdiorController
            String gruop2 = matcher.group(2);
            String gruop5 = matcher.group(5);
            String gruop3 = matcher.group(3);
            String gruop6 = matcher.group(6);
            minNumber = gruop2;
            maxNumber = gruop5;
            minMultiplier = gruop3;
            maxMultiplier = gruop6;

            long min = parseIntWithMultiplier(gruop2, gruop3);
            long max = parseIntWithMultiplier(gruop5, gruop6);

            RangeWithBounds.BoundType minBound = "[".equals(matcher.group(1)) ? RangeWithBounds.BoundType.INCLUDING
                                                                              : RangeWithBounds.BoundType.EXCLUDING;
            RangeWithBounds.BoundType maxBound = "]".equals(matcher.group(7)) ? RangeWithBounds.BoundType.INCLUDING
                                                                              : RangeWithBounds.BoundType.EXCLUDING;

            return new RangeWithBounds(min, max, minBound, maxBound);
        }
    }

    private static final class RangeWithMoreLessSymbolsParser extends BaseRangeParser {
        // >= 5 <= 100
        private static final Pattern PATTERN = Pattern
            .compile("(<=?|>=?)\\s*" + INT_PATTERN + "\\s*(<=?|>=?)\\s*" + INT_PATTERN);

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }
            // TODO need to be refactored. Local variables has been created for thread safe, but they still need for
            // TableEdiorController
            String group2 = matcher.group(2);
            String group5 = matcher.group(5);
            String group3 = matcher.group(3);
            String group6 = matcher.group(6);
            minNumber = group2;
            maxNumber = group5;
            minMultiplier = group3;
            maxMultiplier = group6;

            long first = parseIntWithMultiplier(group2, group3);
            long second = parseIntWithMultiplier(group5, group6);

            String firstBound = matcher.group(1);
            String secondBound = matcher.group(4);

            return getRangeWithBounds(first, second, firstBound, secondBound);
        }

    }

    private static final class VerboseRangeParser extends BaseRangeParser {
        // more than 5 less than 100
        private static final Pattern PATTERN = Pattern.compile(
            "(less\\s+than|more\\s+than)?\\s*" + INT_PATTERN + "\\s*(and\\s+more|or\\s+less)?\\s*(less\\s+than|more\\s+than)?\\s*" + INT_PATTERN + "\\s*(and\\s+more|or\\s+less)?");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }
            // TODO need to be refactored. Local variables has been created for thread safe, but they still need for
            // TableEdiorController
            String group2 = matcher.group(2);
            String group6 = matcher.group(6);
            String group3 = matcher.group(3);
            String group7 = matcher.group(7);
            minNumber = group2;
            maxNumber = group6;
            minMultiplier = group3;
            maxMultiplier = group7;

            long first = parseIntWithMultiplier(group2, group3);
            long second = parseIntWithMultiplier(group6, group7);

            String firstBound1 = matcher.group(1);
            String firstBound2 = matcher.group(4);
            String secondBound1 = matcher.group(5);
            String secondBound2 = matcher.group(8);

            String firstBound = mergeBoundParts(firstBound1, firstBound2);
            String secondBound = mergeBoundParts(secondBound1, secondBound2);

            if (StringUtils.isEmpty(firstBound) || StringUtils.isEmpty(secondBound)) {
                return null;
            }

            firstBound = replaceVerboseToSymbol(firstBound);
            secondBound = replaceVerboseToSymbol(secondBound);

            return getRangeWithBounds(first, second, firstBound, secondBound);
        }

        private String replaceVerboseToSymbol(String bound) {
            return bound.replaceAll("less\\s+than", "<")
                .replaceAll("more\\s+than", ">")
                .replaceAll("or\\s+less", "<=")
                .replaceAll("and\\s+more", ">=");
        }

        private String mergeBoundParts(String part1, String part2) {
            if (StringUtils.isNotEmpty(part1) && StringUtils.isNotEmpty(part2)) {
                // One of them should be empty. Validation failed.
                return null;
            }

            return StringUtils.isEmpty(part1) ? part2 : part1;
        }
    }

}
