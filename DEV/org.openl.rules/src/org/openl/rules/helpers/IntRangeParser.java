package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.source.SourceType;
import org.openl.util.RangeWithBounds;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntRangeParser {
    protected static final BExGrammarParser FALLBACK_PARSER = new BExGrammarParser(SourceType.INT_RANGE);

    private static final IntRangeParser INSTANCE = new IntRangeParser();

    private final static String INT_PATTERN = "\\$?(-?\\d+[,\\d]+\\d+)([KMB]?)";

    protected final RangeParser PARSERS[] = { new SimpleRangeParser(),
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

    public RangeWithBounds parse(String range) {
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
            // Shouldn't occur. But if occurs, log exception and fallback to grammar parser
            Logger log = LoggerFactory.getLogger(RangeWithBounds.class);
            log.error(e.getMessage(), e);
        }

        return FALLBACK_PARSER.parse(range);
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

            int value = parseIntWithMultiplier(number, multiplier);

            return new RangeWithBounds(value, value);
        }
    }

    private static final class PrefixRangeParser extends BaseRangeParser {
        // <= 123M
        private static final Pattern PATTERN = Pattern.compile("(<=?|>=?|less than|more than)\\s*" + INT_PATTERN);

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String prefix = matcher.group(1);
            String number = matcher.group(2);
            String multiplier = matcher.group(3);
            int value = parseIntWithMultiplier(number, multiplier);

            if ("<".equals(prefix) || "less than".equals(prefix)) {
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
            } else if (">".equals(prefix) || "more than".equals(prefix)) {
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
        private static final Pattern PATTERN = Pattern.compile(INT_PATTERN + "\\s*(\\+|and more|or less)");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String number = matcher.group(1);
            String multiplier = matcher.group(2);
            int value = parseIntWithMultiplier(number, multiplier);

            String suffix = matcher.group(3);
            if ("or less".equals(suffix)) {
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

            minNumber = matcher.group(1);
            minMultiplier = matcher.group(2);
            int min = parseIntWithMultiplier(minNumber, minMultiplier);
            String separator = matcher.group(3);
            maxNumber = matcher.group(4);
            maxMultiplier = matcher.group(5);
            int max = parseIntWithMultiplier(maxNumber, maxMultiplier);

            RangeWithBounds.BoundType boundType = "…".equals(separator) || "..."
                .equals(separator) ? RangeWithBounds.BoundType.EXCLUDING : RangeWithBounds.BoundType.INCLUDING;

            return new RangeWithBounds(min, max, boundType, boundType);
        }
    }

    private static final class RangeWithBracketsParser extends BaseRangeParser {
        // [34 - 123)
        private static final Pattern PATTERN = Pattern
            .compile("([\\[\\(])\\s*" + INT_PATTERN + "\\s*([-;…]|\\.{3}|\\.{2}?)\\s*" + INT_PATTERN + "\\s*([\\]\\)])");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            minNumber = matcher.group(2);
            minMultiplier = matcher.group(3);
            int min = parseIntWithMultiplier(minNumber, minMultiplier);
            maxNumber = matcher.group(5);
            maxMultiplier = matcher.group(6);
            int max = parseIntWithMultiplier(maxNumber, maxMultiplier);

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

            minNumber = matcher.group(2);
            minMultiplier = matcher.group(3);
            int first = parseIntWithMultiplier(minNumber, minMultiplier);
            maxNumber = matcher.group(5);
            maxMultiplier = matcher.group(6);
            int second = parseIntWithMultiplier(maxNumber, maxMultiplier);

            String firstBound = matcher.group(1);
            String secondBound = matcher.group(4);

            return getRangeWithBounds(first, second, firstBound, secondBound);
        }

    }

    private static final class VerboseRangeParser extends BaseRangeParser {
        // more than 5 less than 100
        private static final Pattern PATTERN = Pattern.compile(
            "(less than|more than)?\\s*" + INT_PATTERN + "\\s*(and more|or less)?\\s*(less than|more than)?\\s*" + INT_PATTERN + "\\s*(and more|or less)?");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            minNumber = matcher.group(2);
            minMultiplier = matcher.group(3);
            int first = parseIntWithMultiplier(minNumber, minMultiplier);
            maxNumber = matcher.group(6);
            maxMultiplier = matcher.group(7);
            int second = parseIntWithMultiplier(maxNumber, maxMultiplier);

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
            return bound.replace("less than", "<")
                .replace("more than", ">")
                .replace("or less", "<=")
                .replace("and more", ">=");
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
