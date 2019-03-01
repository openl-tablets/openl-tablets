package org.openl.rules.helpers;

import java.util.regex.Pattern;

public final class CharRangeParser extends ARangeParser<Character> {

    private static final CharRangeParser INSTANCE = new CharRangeParser();

    private final RangeParser[] parsers;

    private CharRangeParser() {
        CharacterRangeBoundAdapter adapter = new CharacterRangeBoundAdapter();
        parsers = new RangeParser[] {
                new MinMaxParser<>(Pattern.compile("\\s*(\\S)\\s*([-…]|\\.\\.\\.?)\\s*(\\S)\\s*"), adapter),
                new BracketsParser<>(Pattern.compile("\\s*([\\[(])\\s*(\\S)\\s*(?:;|\\.\\.)\\s*(\\S)\\s*([])])\\s*"),
                    adapter),
                new VerbalParser<>(Pattern.compile("\\s*(\\S)\\s*(\\+|and more|or less)\\s*"), adapter),
                new MoreLessParser<>(Pattern.compile("\\s*(<|>|>=|<=|less than|more than)\\s*(\\S)\\s*"), adapter),
                new RangeWithMoreLessParser<>(Pattern.compile("\\s*(<=?|>=?)\\s*(\\S)\\s*(<=?|>=?)\\s*(\\S)\\s*"),
                    adapter),
                new SimpleParser<>(Pattern.compile("\\s*(\\S)\\s*"), adapter) };
    }

    public static CharRangeParser getInstance() {
        return INSTANCE;
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
