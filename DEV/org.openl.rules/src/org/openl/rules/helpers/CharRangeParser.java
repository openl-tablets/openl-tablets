package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.rules.helpers.CharRangeParser.ParseStruct.BoundType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CharRangeParser {

    private static final CharRangeParser INSTANCE = new CharRangeParser();

    private final RangeParser parsers[] = {
            new MinMaxParser(),
            new BracketsParser(),
            new VerbalParser(),
            new MoreLessParser(),
            new RangeWithMoreLessParser(),
            new SimpleParser()
    };

    private CharRangeParser() {
    }

    public static CharRangeParser getInstance() {
        return INSTANCE;
    }

    public ParseStruct parse(String range) {
        for (RangeParser parser : parsers) {
            try {
                ParseStruct res = parser.parse(range);
                if (res != null) {
                    return res;
                }
            } catch (RuntimeException e) {
                // should not be happened
                Logger log = LoggerFactory.getLogger(getClass());
                log.error(e.getMessage(), e);
            }
        }
        throw new RuntimeException("Invalid Char Range: " + range);
    }

    private static final class SimpleParser implements RangeParser {

        private static final Pattern PATTERN = Pattern.compile("\\s*(\\S)\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m0 = PATTERN.matcher(range);
            if (!m0.matches()) {
                return null;
            }
            String s = m0.group(1);
            return new ParseStruct(s.charAt(0), s.charAt(0));
        }
    }

    private static final class RangeWithMoreLessParser implements RangeParser {

        private static final Pattern PATTERN = Pattern.compile("\\s*(<=?|>=?)\\s*(\\S)\\s*(<=?|>=?)\\s*(\\S)\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m5 = PATTERN.matcher(range);
            if (!m5.matches()) {
                return null;
            }
            String firstBound = m5.group(1);
            String firstChar = m5.group(2);
            String secondBound = m5.group(3);
            String secondChar = m5.group(4);

            BoundType leftBoundType;
            BoundType rightBoundType;
            char min;
            char max;

            if (firstBound.startsWith("<")) {
                if (secondBound.startsWith("<")) {
                    return null;
                }
                min = secondChar.charAt(0);
                max = firstChar.charAt(0);
                leftBoundType = secondBound.equals(">") ? BoundType.EXCLUDING : BoundType.INCLUDING;
                rightBoundType = firstBound.equals("<") ? BoundType.EXCLUDING : BoundType.INCLUDING;
            } else {
                if (secondBound.startsWith(">")) {
                    return null;
                }
                min = firstChar.charAt(0);
                max = secondChar.charAt(0);
                leftBoundType = firstBound.equals(">") ? BoundType.EXCLUDING : BoundType.INCLUDING;
                rightBoundType = secondBound.equals("<") ? BoundType.EXCLUDING : BoundType.INCLUDING;
            }
            return new ParseStruct(min, max, leftBoundType, rightBoundType);
        }
    }

    private static final class MoreLessParser implements RangeParser {

        private static final Pattern PATTERN = Pattern.compile("\\s*(<|>|>=|<=|less than|more than)\\s*(\\S)\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m3 = PATTERN.matcher(range);
            if (!m3.matches()) {
                return null;
            }
            String q = m3.group(1);
            String s = m3.group(2);
            if ("<".equals(q) || "less than".equals(q)) {
                return new ParseStruct(Character.MIN_VALUE, s.charAt(0), BoundType.INCLUDING, BoundType.EXCLUDING);
            }
            if (q.length() > 1 && q.charAt(0) == '<') {
                return new ParseStruct(Character.MIN_VALUE, s.charAt(0));
            }
            if (">".equals(q) || "more than".equals(q)) {
                return new ParseStruct(s.charAt(0), Character.MAX_VALUE, BoundType.EXCLUDING, BoundType.INCLUDING);
            }
            if (q.length() > 1 && q.charAt(0) == '>') {
                return new ParseStruct(s.charAt(0), Character.MAX_VALUE);
            }
            return null;
        }
    }

    private static final class VerbalParser implements RangeParser {

        private static final Pattern PATTERN = Pattern.compile("\\s*(\\S)\\s*(\\+|and more|or less)\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m2 = PATTERN.matcher(range);
            if (!m2.matches()) {
                return null;
            }
            String s = m2.group(1);
            String suffix = m2.group(2);
            if ("or less".equals(suffix)) {
                return new ParseStruct(Character.MIN_VALUE, s.charAt(0));
            }
            return new ParseStruct(s.charAt(0), Character.MAX_VALUE);
        }
    }

    private static final class BracketsParser implements RangeParser {

        private static final Pattern PATTERN = Pattern
            .compile("\\s*([\\[(])\\s*(\\S)\\s*(?:;|\\.\\.)\\s*(\\S)\\s*([])])\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m4 = PATTERN.matcher(range);
            if (!m4.matches()) {
                return null;
            }
            String s1 = m4.group(2);
            String s2 = m4.group(3);

            String lb = m4.group(1);
            BoundType leftBoundType = BoundType.INCLUDING;
            BoundType rightBoundType = BoundType.INCLUDING;
            if (lb.charAt(0) == '(') {
                leftBoundType = BoundType.EXCLUDING;
            }
            String rb = m4.group(4);
            if (rb.charAt(0) == ')') {
                rightBoundType = BoundType.EXCLUDING;
            }
            return new ParseStruct(s1.charAt(0), s2.charAt(0), leftBoundType, rightBoundType);
        }
    }

    private static final class MinMaxParser implements RangeParser {

        private static final Pattern PATTERN = Pattern.compile("\\s*(\\S)\\s*([-…]|\\.\\.\\.?)\\s*(\\S)\\s*");

        @Override
        public ParseStruct parse(String range) {
            Matcher m1 = PATTERN.matcher(range);
            if (!m1.matches()) {
                return null;
            }
            String s1 = m1.group(1);
            String s2 = m1.group(3);

            String delim = m1.group(2);
            if (delim.equals("...") || delim.equals("…")) {
                return new ParseStruct(s1.charAt(0), s2.charAt(0), BoundType.EXCLUDING, BoundType.EXCLUDING);
            }
            return new ParseStruct(s1.charAt(0), s2.charAt(0));
        }
    }

    private interface RangeParser {
        ParseStruct parse(String range);
    }

    static final class ParseStruct {

        enum BoundType {
            INCLUDING,
            EXCLUDING
        }

        char min, max;

        BoundType leftBoundType;
        BoundType rightBoundType;

        public ParseStruct(char min, char max) {
            this(min, max, BoundType.INCLUDING, BoundType.INCLUDING);
        }

        public ParseStruct(char min, char max, BoundType leftBoundType, BoundType rightBoundType) {
            this.min = min;
            this.max = max;
            this.leftBoundType = leftBoundType;
            this.rightBoundType = rightBoundType;
        }
    }

}
