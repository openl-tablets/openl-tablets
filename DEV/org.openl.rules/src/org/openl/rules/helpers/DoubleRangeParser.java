package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.openl.source.SourceType;
import org.openl.util.RangeWithBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoubleRangeParser {
    private static final BExGrammarParser FALLBACK_PARSER = new BExGrammarParser(SourceType.DOUBLE_RANGE);

    private static final RangeParser PARSERS[] = {
            new SimpleRangeParser(),
            new RangeWithBracketsParser(),
            new PrefixRangeParser(),
            new SuffixRangeParser(),
            new NumberParser(),
            new RangeWithMoreLessSymbolsParser(),
            new VerboseRangeParser()
    };

    public static RangeWithBounds parse(String range) {
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
        // Just a simple number like "$-55,123.4K" (minus 55123.4 thousands)
        private static final Pattern PATTERN = Pattern.compile("\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double value = parseDoubleWithMultiplier(matcher.group(1), matcher.group(2));

            return new RangeWithBounds(value, value);
        }
    }

    private static final class PrefixRangeParser extends BaseRangeParser {
        // <= 123.4M
        private static final Pattern PATTERN = Pattern.compile("(<=?|>=?|less than|more than)\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            String prefix = matcher.group(1);
            double value = parseDoubleWithMultiplier(matcher.group(2), matcher.group(3));

            if ("<".equals(prefix) || "less than".equals(prefix)) {
                return new RangeWithBounds(getMin(value),
                        value,
                        RangeWithBounds.BoundType.INCLUDING,
                        RangeWithBounds.BoundType.EXCLUDING);
            } else if ("<=".equals(prefix)) {
                return new RangeWithBounds(getMin(value),
                        value,
                        RangeWithBounds.BoundType.INCLUDING,
                        RangeWithBounds.BoundType.INCLUDING);
            } else if (">".equals(prefix) || "more than".equals(prefix)) {
                return new RangeWithBounds(value,
                        getMax(value),
                        RangeWithBounds.BoundType.EXCLUDING,
                        RangeWithBounds.BoundType.INCLUDING);
            } else if (">=".equals(prefix)) {
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
        // 34.5+
        private static final Pattern PATTERN = Pattern.compile("\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*(\\+|and more|or less)");

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double value = parseDoubleWithMultiplier(matcher.group(1), matcher.group(2));

            String suffix = matcher.group(3);
            if ("or less".equals(suffix)) {
                return new RangeWithBounds(getMin(value),
                        value,
                        RangeWithBounds.BoundType.INCLUDING,
                        RangeWithBounds.BoundType.INCLUDING);
            } else {
                return new RangeWithBounds(value,
                        getMax(value),
                        RangeWithBounds.BoundType.INCLUDING,
                        RangeWithBounds.BoundType.INCLUDING);
            }
        }
    }

    private static final class SimpleRangeParser extends BaseRangeParser {
        // 34.5 - 123.6
        private static final Pattern PATTERN = Pattern.compile(
                "\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*([-;…]|\\.\\.\\.?)\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)"
        );

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double min = parseDoubleWithMultiplier(matcher.group(1), matcher.group(2));
            String separator = matcher.group(3);
            double max = parseDoubleWithMultiplier(matcher.group(4), matcher.group(5));

            RangeWithBounds.BoundType boundType = "…".equals(separator) || "...".equals(separator) ?
                                                  RangeWithBounds.BoundType.EXCLUDING :
                                                  RangeWithBounds.BoundType.INCLUDING;

            return new RangeWithBounds(min, max, boundType, boundType);
        }
    }

    private static final class RangeWithBracketsParser extends BaseRangeParser {
        // [34.5 - 123.6)
        private static final Pattern PATTERN = Pattern.compile(
                "([\\[\\(])\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*([-;…]|\\.\\.\\.?)\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*([\\]\\)])"
        );

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double min = parseDoubleWithMultiplier(matcher.group(2), matcher.group(3));
            double max = parseDoubleWithMultiplier(matcher.group(5), matcher.group(6));

            RangeWithBounds.BoundType minBound = "[".equals(matcher.group(1)) ? RangeWithBounds.BoundType.INCLUDING :
                                                  RangeWithBounds.BoundType.EXCLUDING;
            RangeWithBounds.BoundType maxBound = "]".equals(matcher.group(7)) ? RangeWithBounds.BoundType.INCLUDING :
                                                   RangeWithBounds.BoundType.EXCLUDING;

            return new RangeWithBounds(min, max, minBound, maxBound);
        }
    }

    private static final class RangeWithMoreLessSymbolsParser extends BaseRangeParser {
        // >= 5.1 <= 100.2
        private static final Pattern PATTERN = Pattern.compile(
                "(<=?|>=?)\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*(<=?|>=?)\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)"
        );

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double first = parseDoubleWithMultiplier(matcher.group(2), matcher.group(3));
            double second = parseDoubleWithMultiplier(matcher.group(5), matcher.group(6));

            String firstBound = matcher.group(1);
            String secondBound = matcher.group(4);

            return getRangeWithBounds(first, second, firstBound, secondBound);
        }

    }

    private static final class VerboseRangeParser extends BaseRangeParser {
        // more than 5.1 less than 100.2
        private static final Pattern PATTERN = Pattern.compile(
                "(less than|more than)?\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*(and more|or less)?\\s*(less than|more than)?\\s*\\$?(-?[,\\d]+\\.?\\d*)([KMB]?)\\s*(and more|or less)?"
        );

        @Override
        public RangeWithBounds parse(String range) {
            Matcher matcher = PATTERN.matcher(range);
            if (!matcher.matches()) {
                return null;
            }

            double first = parseDoubleWithMultiplier(matcher.group(2), matcher.group(3));
            double second = parseDoubleWithMultiplier(matcher.group(6), matcher.group(7));

            String firstBound1 = matcher.group(1);
            String firstBound2 = matcher.group(4);
            String secondBound1 = matcher.group(5);
            String secondBound2 = matcher.group(8);

            String firstBound = mergeBoundParts(firstBound1, firstBound2);
            String secondBound = mergeBoundParts(secondBound1, secondBound2);

            if (StringUtils.isAnyEmpty(firstBound, secondBound)) {
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
            if (!StringUtils.isAnyEmpty(part1, part2)) {
                // One of them should be empty. Validation failed.
                return null;
            }

            return StringUtils.isEmpty(part1) ? part2 : part1;
        }
    }
}
