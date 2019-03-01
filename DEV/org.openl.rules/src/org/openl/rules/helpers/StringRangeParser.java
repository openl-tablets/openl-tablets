package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class StringRangeParser extends ARangeParser<String> {

    public static final String MAX_VALUE = String.valueOf(Character.MAX_VALUE);
    public static final String MIN_VALUE = " ";

    private static final StringRangeParser INSTANCE = new StringRangeParser();

    private final RangeParser[] parsers;

    private StringRangeParser() {
        StringRangeBoundAdapter adapter = new StringRangeBoundAdapter();
        parsers = new RangeParser[] {
                new BracketsParser<>(Pattern.compile("\\s*([\\[(])\\s*(\\S+)\\s*(?:;|\\.\\.)\\s*(\\S+)\\s*([])])\\s*"),
                    adapter),
                new MinMaxParser<>(Pattern.compile("\\s*(\\S+)\\s*([-…]|\\.\\.\\.?)\\s*(\\S+)\\s*"), adapter),
                new VerbalParser<>(Pattern.compile("\\s*(\\S+)\\s*(\\+|and more|or less)\\s*"), adapter),
                new MoreLessParser<>(Pattern.compile("\\s*(<|>|>=|<=|less than|more than)\\s*(\\S+)\\s*"), adapter),
                new RangeWithMoreLessParser<>(Pattern.compile("\\s*(<=?|>=?)\\s*(\\S+)\\s*(<=?|>=?)\\s*(\\S+)\\s*"),
                    adapter),
                new SimpleParser<>(Pattern.compile("\\s*(\\S+)\\s*"), adapter) };
    }

    public boolean isStringRange(String s) {
        return parsers[0].parse(s) != null;
    }

    public static StringRangeParser getInstance() {
        return INSTANCE;
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
