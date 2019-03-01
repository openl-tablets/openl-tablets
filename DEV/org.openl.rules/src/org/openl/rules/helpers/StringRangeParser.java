package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class StringRangeParser extends ARangeParser<String> {

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
            return null;
        }

        @Override
        public String getMaxRightBound() {
            return null;
        }
    }

}
