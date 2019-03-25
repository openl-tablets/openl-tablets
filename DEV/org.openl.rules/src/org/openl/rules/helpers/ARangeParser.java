package org.openl.rules.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ARangeParser<T> {

    ARangeParser() {
    }

    abstract RangeParser[] getRangeParsers();

    @SuppressWarnings("unchecked")
    public ParseStruct<T> parse(String range) {
        for (RangeParser parser : getRangeParsers()) {
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
        throw new RuntimeException("Invalid Range: " + range);
    }

    static final class SimpleParser<T> extends AParser<T> {

        SimpleParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        ParseStruct<T> doParse(Matcher m) {
            T o = adapter.adaptValue(m.group(1));
            return new ParseStruct<>(o, o);
        }
    }

    static final class MinMaxParser<T> extends AParser<T> {

        MinMaxParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        ParseStruct<T> doParse(Matcher m) {
            T o1 = adapter.adaptValue(m.group(1));
            T o2 = adapter.adaptValue(m.group(3));

            String delim = m.group(2);
            if (delim.equals("...") || delim.equals("…")) {
                return new ParseStruct<>(o1, o2, BoundType.EXCLUDING, BoundType.EXCLUDING);
            }
            return new ParseStruct<>(o1, o2);
        }
    }

    static final class BracketsParser<T> extends AParser<T> {

        BracketsParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        public ParseStruct<T> doParse(Matcher m) {
            T o1 = adapter.adaptValue(m.group(2));
            T o2 = adapter.adaptValue(m.group(3));

            String lb = m.group(1);
            BoundType leftBoundType = BoundType.INCLUDING;
            BoundType rightBoundType = BoundType.INCLUDING;
            if (lb.charAt(0) == '(') {
                leftBoundType = BoundType.EXCLUDING;
            }
            String rb = m.group(4);
            if (rb.charAt(0) == ')') {
                rightBoundType = BoundType.EXCLUDING;
            }
            return new ParseStruct<>(o1, o2, leftBoundType, rightBoundType);
        }
    }

    static final class VerbalParser<T> extends AParser<T> {

        VerbalParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        public ParseStruct<T> doParse(Matcher m) {
            T s = adapter.adaptValue(m.group(1));
            String suffix = m.group(2);
            if ("or less".equals(suffix)) {
                return new ParseStruct<>(adapter.getMinLeftBound(), s);
            }
            return new ParseStruct<>(s, adapter.getMaxRightBound());
        }
    }

    static final class MoreLessParser<T> extends AParser<T> {

        MoreLessParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        public ParseStruct<T> doParse(Matcher m) {
            String q = m.group(1);
            T o = adapter.adaptValue(m.group(2));
            if ("<".equals(q) || "less than".equals(q)) {
                return new ParseStruct<>(adapter.getMinLeftBound(), o, BoundType.INCLUDING, BoundType.EXCLUDING);
            }
            if (q.length() > 1 && q.charAt(0) == '<') {
                return new ParseStruct<>(adapter.getMinLeftBound(), o);
            }
            if (">".equals(q) || "more than".equals(q)) {
                return new ParseStruct<>(o, adapter.getMaxRightBound(), BoundType.EXCLUDING, BoundType.INCLUDING);
            }
            if (q.length() > 1 && q.charAt(0) == '>') {
                return new ParseStruct<>(o, adapter.getMaxRightBound());
            }
            return null;
        }
    }

    static final class RangeWithMoreLessParser<T> extends AParser<T> {

        RangeWithMoreLessParser(Pattern pattern, RangeBoundAdapter<T> converter) {
            super(pattern, converter);
        }

        @Override
        public ParseStruct<T> doParse(Matcher m) {
            String firstBound = m.group(1);
            T first = adapter.adaptValue(m.group(2));
            String secondBound = m.group(3);
            T second = adapter.adaptValue(m.group(4));

            BoundType leftBoundType;
            BoundType rightBoundType;
            T min;
            T max;

            if (firstBound.startsWith("<")) {
                if (secondBound.startsWith("<")) {
                    return null;
                }
                min = second;
                max = first;
                leftBoundType = secondBound.equals(">") ? BoundType.EXCLUDING : BoundType.INCLUDING;
                rightBoundType = firstBound.equals("<") ? BoundType.EXCLUDING : BoundType.INCLUDING;
            } else {
                if (secondBound.startsWith(">")) {
                    return null;
                }
                min = first;
                max = second;
                leftBoundType = firstBound.equals(">") ? BoundType.EXCLUDING : BoundType.INCLUDING;
                rightBoundType = secondBound.equals("<") ? BoundType.EXCLUDING : BoundType.INCLUDING;
            }
            return new ParseStruct<>(min, max, leftBoundType, rightBoundType);
        }
    }

    private static abstract class AParser<T> implements RangeParser<T> {

        private final Pattern pattern;
        final RangeBoundAdapter<T> adapter;

        AParser(Pattern pattern, RangeBoundAdapter<T> adapter) {
            this.pattern = pattern;
            this.adapter = adapter;
        }

        @Override
        public ParseStruct<T> parse(String range) {
            Matcher m = pattern.matcher(range);
            if (!m.matches()) {
                return null;
            }
            return doParse(m);
        }

        abstract ParseStruct<T> doParse(Matcher m);
    }

    interface RangeBoundAdapter<T> {

        T adaptValue(String s);

        T getMinLeftBound();

        T getMaxRightBound();
    }

    interface RangeParser<T> {
        ParseStruct<T> parse(String range);
    }

    public static final class ParseStruct<T> {

        public enum BoundType {
            INCLUDING,
            EXCLUDING
        }

        final T min, max;

        final BoundType leftBoundType;
        final BoundType rightBoundType;

        ParseStruct(T min, T max) {
            this(min, max, BoundType.INCLUDING, BoundType.INCLUDING);
        }

        ParseStruct(T min, T max, BoundType leftBoundType, BoundType rightBoundType) {
            this.min = min;
            this.max = max;
            this.leftBoundType = leftBoundType;
            this.rightBoundType = rightBoundType;
        }
    }

}
