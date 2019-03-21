package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public final class StringRangeParser extends ARangeParser<String> {

    public static final String MAX_VALUE = String.valueOf(Character.MAX_VALUE);
    public static final String MIN_VALUE = StringUtils.SPACE;

    private static class StringRangeParserHolder {
        private static final StringRangeParser INSTANCE = new StringRangeParser();
    }

    private final RangeParser[] parsers;
    private final Pattern[] patterns;
    private final Pattern skipPattern;
    
    private final static String SKIP_PATTERN = "\\s*(\\S+)\\s*-\\s*(\\S+)\\s*";
    
    private final static String BRACKETS_PATTERN = "\\s*([\\[(])\\s*(\\S+)\\s*(?:[-;…]|\\.{3}|\\.{2})\\s*(\\S+)\\s*([])])\\s*";
    private final static String MIN_MAX_PATTERN = "\\s*(\\S+)\\s*([-…]|\\.{3}|\\.{2})\\s*(\\S+)\\s*";
    private final static String VERBAL_PATTERN = "\\s*(\\S+)\\s*(\\+|and more|or less)\\s*";
    private final static String MORE_LESS_PATTERN = "\\s*(<|>|>=|<=|less than|more than)\\s*(\\S+)\\s*";
    private final static String RANGE_MORE_LESS_PATTERN = "\\s*(<=?|>=?)\\s*(\\S+)\\s*(<=?|>=?)\\s*(\\S+)\\s*";
    private final static String SIMPLE_PATTERN = "\\s*(\\S+)\\s*";

    private StringRangeParser() {
        StringRangeBoundAdapter adapter = new StringRangeBoundAdapter();
        patterns = new Pattern[] { Pattern.compile(BRACKETS_PATTERN),
                Pattern.compile(MIN_MAX_PATTERN),
                Pattern.compile(VERBAL_PATTERN),
                Pattern.compile(MORE_LESS_PATTERN),
                Pattern.compile(RANGE_MORE_LESS_PATTERN),
                Pattern.compile(SIMPLE_PATTERN)};
        skipPattern = Pattern.compile(SKIP_PATTERN);
        parsers = new RangeParser[] { new BracketsParser<>(patterns[0], adapter),
                new MinMaxParser<>(patterns[1], adapter),
                new VerbalParser<>(patterns[2], adapter),
                new MoreLessParser<>(patterns[3], adapter),
                new RangeWithMoreLessParser<>(patterns[4], adapter),
                new SimpleParser<>(patterns[5], adapter) };
    }

    public boolean isStringRange(String value) {
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(value);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean canBeNotStringRange(String value) {
        Matcher m = patterns[5].matcher(value);
        if (m.matches()) {
            return true;
        }
        m = skipPattern.matcher(value);
        if (m.matches()) {
            return true;
        }
        return false;
    }

    public static StringRangeParser getInstance() {
        return StringRangeParserHolder.INSTANCE;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class StringRangeBoundAdapter implements RangeBoundAdapter<String> {

        @Override
        public String adaptValue(String s) {
            return s;
        }

        @Override
        public String getMinLeftBound() {
            return MIN_VALUE;
        }

        @Override
        public String getMaxRightBound() {
            return MAX_VALUE;
        }
    }

}
