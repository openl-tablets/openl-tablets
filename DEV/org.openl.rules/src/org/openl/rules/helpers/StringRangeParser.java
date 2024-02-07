package org.openl.rules.helpers;

import java.text.ParseException;

import org.openl.rules.range.RangeParser;

public final class StringRangeParser {

    public static final String MAX_VALUE = String.valueOf(Character.MAX_VALUE);
    public static final String MIN_VALUE = String.valueOf(Character.MIN_VALUE);

    private static class StringRangeParserHolder {
        private static final StringRangeParser INSTANCE = new StringRangeParser();
    }

    private StringRangeParser() {
    }

    public boolean likelyRangeThanString(String value) {
        try {
            return RangeParser.parse(value) != null;
        } catch (ParseException e) {
            return false;
        }
    }

    public static StringRangeParser getInstance() {
        return StringRangeParserHolder.INSTANCE;
    }

}
