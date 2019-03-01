package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class StringRangeParser extends ARangeParser<String> {

    private static final StringRangeParser INSTANCE = new StringRangeParser();

    private final RangeParser[] parsers;

    private StringRangeParser() {
        StringConverter converter = new StringConverter();
        parsers = new RangeParser[] {
                new BracketsParser<>(Pattern.compile("\\s*([\\[(])\\s*(\\S+)\\s*(?:;|\\.\\.)\\s*(\\S+)\\s*([])])\\s*"),
                    converter),
                new MinMaxParser<>(Pattern.compile("\\s*(\\S+)\\s*([-…]|\\.\\.\\.?)\\s*(\\S+)\\s*"), converter),
                new VerbalParser<>(Pattern.compile("\\s*(\\S+)\\s*(\\+|and more|or less)\\s*"), converter),
                new MoreLessParser<>(Pattern.compile("\\s*(<|>|>=|<=|less than|more than)\\s*(\\S+)\\s*"), converter),
                new RangeWithMoreLessParser<>(Pattern.compile("\\s*(<=?|>=?)\\s*(\\S+)\\s*(<=?|>=?)\\s*(\\S+)\\s*"),
                    converter),
                new SimpleParser<>(Pattern.compile("\\s*(\\S+)\\s*"), converter) };
    }

    public static StringRangeParser getInstance() {
        return INSTANCE;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class StringConverter implements Converter<String> {

        @Override
        public String convert(String s) {
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
