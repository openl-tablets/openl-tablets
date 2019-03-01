package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class CharRangeParser extends ARangeParser<Character> {

    private static final CharRangeParser INSTANCE = new CharRangeParser();

    private final RangeParser[] parsers;

    private CharRangeParser() {
        CharacterConverter converter = new CharacterConverter();
        parsers = new RangeParser[] {
                new MinMaxParser<>(Pattern.compile("\\s*(\\S)\\s*([-…]|\\.\\.\\.?)\\s*(\\S)\\s*"), converter),
                new BracketsParser<>(Pattern.compile("\\s*([\\[(])\\s*(\\S)\\s*(?:;|\\.\\.)\\s*(\\S)\\s*([])])\\s*"),
                    converter),
                new VerbalParser<>(Pattern.compile("\\s*(\\S)\\s*(\\+|and more|or less)\\s*"), converter),
                new MoreLessParser<>(Pattern.compile("\\s*(<|>|>=|<=|less than|more than)\\s*(\\S)\\s*"), converter),
                new RangeWithMoreLessParser<>(Pattern.compile("\\s*(<=?|>=?)\\s*(\\S)\\s*(<=?|>=?)\\s*(\\S)\\s*"),
                    converter),
                new SimpleParser<>(Pattern.compile("\\s*(\\S)\\s*"), converter) };
    }

    public static CharRangeParser getInstance() {
        return INSTANCE;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class CharacterConverter implements Converter<Character> {

        @Override
        public Character convert(String s) {
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
