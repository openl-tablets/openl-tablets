package org.openl.rules.range;

import java.text.ParseException;

/**
 * A performance and memory optimized parser for the range.Support the following patterns:
 * [ X; Y ]
 * <=X > Y
 * > X
 * X +
 * X and more
 * X or less
 * less than X
 * more than X
 * X ... Y
 * X
 *
 * @author Yury Molchan
 */
public class RangeParser {

    Range.Type type;
    String left, right;

    private RangeParser(Range.Type type, String left, String right) {
        this.type = type;
        this.left = left;
        this.right = right;
    }

    RangeParser(Range.Type type, String text) {
        this.type = type;
        this.left = type.left == Range.Bound.UNBOUND ? null : text;
        this.right = type.right == Range.Bound.UNBOUND ? null : text;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(20);
        Range.Type type = getType();
        switch (type) {
            case OPEN:
            case CLOSED:
            case OPEN_CLOSED:
            case CLOSED_OPEN:
                s.append(type.left == Range.Bound.OPEN ? '(' : '[');
                s.append(left).append(" .. ").append(right);
                s.append(type.right == Range.Bound.OPEN ? ')' : ']');
                break;
            case LEFT_OPEN:
                s.append(">").append(left);
                break;
            case LEFT_CLOSED:
                s.append(">=").append(left);
                break;
            case RIGHT_OPEN:
                s.append("<").append(right);
                break;
            case RIGHT_CLOSED:
                s.append("<=").append(right);
                break;
            default:
                s.append(type).append(": ").append(left).append(" - ").append(right);
        }
        return s.toString();
    }

    public Range.Type getType() {
        return type;
    }

    public String getLeft() {
        return left;
    }

    public String getRight() {
        return right;
    }

    public static RangeParser parse(String text) throws ParseException {
        int first = 0;
        int last = text.length() - 1;
        //trim from the first
        first = nextNonSpace(text, first, last);
        //trim from the last
        last = prevNonSpace(text, first, last);

        char ch = text.charAt(first);
        char ch2 = text.charAt(last);
        int index;

        // bracket form: [x; y]
        if (ch == '[' || ch == '(') {
            if (ch2 != ']' && ch2 != ')') {
                throw new ParseException("An illegal opening bracket without closing", first);
            }

            //trim from the first
            first++;
            first = nextNonSpace(text, first, last);

            //trim from the last
            last--;
            last = prevNonSpace(text, first, last);

            RangeParser result = parseRangeBySeparator(text, first, last);
            if (result == null) {
                throw new ParseException("No required bounds separator is found inside the range", first);
            }
            if (ch == '(') {
                if (ch2 == ')') {
                    result.type = Range.Type.OPEN;
                } else {
                    result.type = Range.Type.OPEN_CLOSED;
                }
            } else {
                if (ch2 == ')') {
                    result.type = Range.Type.CLOSED_OPEN;
                } else {
                    result.type = Range.Type.CLOSED;
                }
            }

            return result;
        }

        // comparable form: >=x < y
        if (ch == '>' || ch == '<') {
            first++;
            if (first > last) {
                throw new ParseException("No comparable value", last);
            }
            ch2 = text.charAt(first);
            Range.Type type;
            if (ch2 == '=') {
                if (ch == '>') {
                    type = Range.Type.LEFT_CLOSED;
                } else {
                    type = Range.Type.RIGHT_CLOSED;
                }
                first++;
            } else {
                if (ch == '>') {
                    type = Range.Type.LEFT_OPEN;
                } else {
                    type = Range.Type.RIGHT_OPEN;
                }

            }
            first = nextNonSpace(text, first, last);
            int first2;
            int last2 = last;
            for (int i = first; i <= last; i++) {
                ch2 = text.charAt(i);
                if (ch2 == '>' || ch2 == '<') {
                    if (ch == ch2) {
                        throw new ParseException("Duplicated comparison sign is found in the range", i);
                    }
                    last = prevNonSpace(text, first, i - 1);
                    first2 = i;
                    first2++;

                    if (first2 > last2) {
                        throw new ParseException("No comparable value for the second comparison sign", last2);
                    }
                    ch = text.charAt(first2);
                    if (ch == '=') {
                        first2++;
                    }
                    first2 = nextNonSpace(text, first2, last2);

                    if (ch2 == '>') {
                        // swap the order
                        i = first;
                        first = first2;
                        first2 = i;

                        i = last;
                        last = last2;
                        last2 = i;
                    }

                    switch (type) {
                        case LEFT_CLOSED:
                            type = ch == '=' ? Range.Type.CLOSED : Range.Type.CLOSED_OPEN;
                            break;
                        case RIGHT_CLOSED:
                            type = ch == '=' ? Range.Type.CLOSED : Range.Type.OPEN_CLOSED;
                            break;
                        case LEFT_OPEN:
                            type = ch == '=' ? Range.Type.OPEN_CLOSED : Range.Type.OPEN;
                            break;
                        case RIGHT_OPEN:
                            type = ch == '=' ? Range.Type.CLOSED_OPEN : Range.Type.OPEN;
                            break;
                        default:
                            throw new IllegalStateException(type.name());
                    }
                    return new RangeParser(type, text.substring(first, last + 1), text.substring(first2, last2 + 1));
                }

            }
            return new RangeParser(type, text.substring(first, last + 1));
        }


        // suffix form: x+
        if (ch2 == '+') {
            last--;
            last = prevNonSpace(text, first, last);
            return new RangeParser(Range.Type.LEFT_CLOSED, text.substring(first, last + 1));
        }


        // suffix form: y and more
        index = findPrevWord(" and more", text, first, last);
        if (index >= 0) {
            return new RangeParser(Range.Type.LEFT_CLOSED, text.substring(first, index + 1));
        }

        // suffix form: x or less
        index = findPrevWord(" or less", text, first, last);
        if (index >= 0) {
            return new RangeParser(Range.Type.RIGHT_CLOSED, text.substring(first, index + 1));
        }

        // prefix form: less than x
        index = findNextWord("less than ", text, first, last);
        if (index >= 0) {
            return new RangeParser(Range.Type.RIGHT_OPEN, text.substring(index, last + 1));
        }

        // prefix form: more than y
        index = findNextWord("more than ", text, first, last);
        if (index >= 0) {
            return new RangeParser(Range.Type.LEFT_OPEN, text.substring(index, last + 1));
        }

        // range form: x .. y
        return parseRangeBySeparator(text, first, last);
    }

    private static RangeParser parseRangeBySeparator(String text, int first, int last) throws ParseException {
        int index = findSep(text, first, last);
        if (index < 0) {
            return null;
        }
        int sepLeft;
        int sepRight;
        sepLeft = prevNonSpace(text, first, index - 1);
        Separator sep = Separator.recognize(text, index);
        index += sep.length();
        if (index > last) {
            throw new ParseException("No required bounds separator is found inside the range", last);
        }
        sepRight = nextNonSpace(text, index, last);

        if (!Character.isWhitespace(text.charAt(sepLeft + 1)) || !Character.isWhitespace(text.charAt(index))) {
            // try to find more suitable separator surrounded with spaces
            index = sepRight - 1;
            while (index < last) {
                index = findSep(text, index, last);
                if (index < 0) {
                    // not found
                    break;
                }
                if (!Character.isWhitespace(text.charAt(index - 1))) {
                    // no prefixed whitespace
                    continue;
                }
                var prev = index;
                Separator sep2 = Separator.recognize(text, index);
                index += sep2.length();
                if (index < last && Character.isWhitespace(text.charAt(index))) {
                    // found
                    sep = sep2;
                    sepLeft = prevNonSpace(text, first, prev - 1);
                    sepRight = nextNonSpace(text, index, last);
                    break;
                }
            }
        }
        return new RangeParser(sep.getType(), text.substring(first, sepLeft + 1), text.substring(sepRight, last + 1));
    }

    private static int nextNonSpace(CharSequence text, int start, int end) throws ParseException {
        int index = findNonSpace(text, start, end);
        if (index < 0) {
            throw new ParseException("Unexpected whitespace", start);
        }
        return index;
    }

    private static int prevNonSpace(CharSequence text, int start, int end) throws ParseException {
        while (Character.isWhitespace(text.charAt(end))) {
            end--;
            if (start > end) {
                throw new ParseException("Unexpected whitespace", start);
            }
        }
        return end;

    }

    private static int findNonSpace(CharSequence text, int start, int end) {
        if (start == -1) {
            return -1;
        }
        for (int i = start; i <= end; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findSep(CharSequence text, int start, int end) {
        if (start == -1) {
            return -1;
        }
        // shift boundaries for searching a separator.
        // the first and the last symbols are always values between which separator is defined.
        start = start + 1;
        for (int i = start; i < end; i++) {
            switch (text.charAt(i)) {
                case ';':
                case '-':
                case '…':
                    return i;
                case '.':
                    // the separator is at least two dots
                    if (i + 1 < end && text.charAt(i + 1) == '.') {
                        return i;
                    }
            }
        }
        //not found
        return -1;
    }

    private static int findNextWord(CharSequence word, CharSequence text, int start, int end) {
        if (start == -1) {
            return -1;
        }
        int length = word.length();
        if (start + length > end) {
            // Not matched by length
            return -1;
        }
        for (int i = 0; i < length; i++) {
            char ch = word.charAt(i);
            char at = text.charAt(start);
            start++;
            if (Character.isWhitespace(ch)) {
                if (!Character.isWhitespace(at)) {
                    return -1;
                }
                start = findNonSpace(text, start, end);
                if (start < 0) {
                    return -1;
                }
            } else if (at != ch) {
                return -1;
            }
        }
        return start;
    }

    private static int findPrevWord(CharSequence word, CharSequence text, int start, int end) {
        int length = word.length();
        if (start + length > end) {
            // Not matched by length
            return -1;
        }
        for (int i = length - 1; i >= 0; i--) {
            char ch = word.charAt(i);
            char at = text.charAt(end);
            end--;
            if (Character.isWhitespace(ch)) {
                if (!Character.isWhitespace(at)) {
                    return -1;
                }
                try {
                    end = prevNonSpace(text, start, end);
                } catch (ParseException ex) {
                    return -1;
                }
            } else if (at != ch) {
                return -1;
            }
        }
        return end;
    }

}
