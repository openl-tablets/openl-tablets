package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class CharRangeParser extends ARangeParser<Character> {

    private static class CharRangeParserHolder {
        private static final CharRangeParser INSTANCE = new CharRangeParser();
    }

    private final RangeParser[] parsers;
    private final Pattern[] patterns;

    private final static String MIN_MAX_PATTERN = "\\s*(\\S)\\s*([-;…]|\\.{3}|\\.{2})\\s*(\\S)\\s*";
    private final static String BRACKETS_PATTERN = "\\s*([\\[(])\\s*(\\S)\\s*(?:[-;…]|\\.{3}|\\.{2})\\s*(\\S)\\s*([])])\\s*";
    private final static String VERBAL_PATTERN = "\\s*(\\S)\\s*(\\+|and\\s+more|or\\s+less)\\s*";
    private final static String MORE_LESS_PATTERN = "\\s*(<|>|>=|<=|less\\s+than|more\\s+than)\\s*(\\S)\\s*";
    private final static String RANGE_MORE_LESS_PATTERN = "\\s*(<=?|>=?)\\s*(\\S)\\s*(<=?|>=?)\\s*(\\S)\\s*";
    private final static String SIMPLE_PATTERN = "\\s*(\\S)\\s*";

    private CharRangeParser() {
        CharacterRangeBoundAdapter adapter = new CharacterRangeBoundAdapter();
        patterns = new Pattern[] { Pattern.compile(BRACKETS_PATTERN),
                Pattern.compile(MIN_MAX_PATTERN),
                Pattern.compile(VERBAL_PATTERN),
                Pattern.compile(MORE_LESS_PATTERN),
                Pattern.compile(RANGE_MORE_LESS_PATTERN),
                Pattern.compile(SIMPLE_PATTERN) };

        parsers = new RangeParser[] { new BracketsParser<>(patterns[0], adapter),
                new MinMaxParser<>(patterns[1], adapter),
                new VerbalParser<>(patterns[2], adapter),
                new MoreLessParser<>(patterns[3], adapter),
                new RangeWithMoreLessParser<>(patterns[4], adapter),
                new SimpleParser<>(patterns[5], adapter) };
    }

    public static CharRangeParser getInstance() {
        return CharRangeParserHolder.INSTANCE;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class CharacterRangeBoundAdapter implements RangeBoundAdapter<Character> {

        @Override
        public Character adaptValue(String s) {
            return s.charAt(0);
        }

        @Override
        public Character getMinLeftBound() {
            return Character.MIN_VALUE;
        }

        @Override
        public Character getMaxRightBound() {
            return Character.MAX_VALUE;
        }
    }

}
