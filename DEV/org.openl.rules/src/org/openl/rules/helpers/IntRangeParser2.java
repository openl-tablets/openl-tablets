package org.openl.rules.helpers;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class IntRangeParser2 {
    public static final String ERROR_OUT_RANGE = "Integer value is out of possible range";
    public static final String ERROR_NOT_NUMBER = "Number expected";
    static final int RANGE = 1000000, RANGE_EXCLUSIVE = RANGE + 1, LE = RANGE_EXCLUSIVE + 1, GE = LE + 1, ID = GE + 1,
            KEYWORD = ID + 1, INT_VALUE = KEYWORD + 1, ILLEGAL = Integer.MAX_VALUE - 1, EOS = Integer.MAX_VALUE;
    static final int KW_AND = 1000, KW_LESS = 1001, KW_MORE = 1002, KW_OR = 1003, KW_THAN = 1004;
    static final Map<String, Integer> keywords;
    static {
        keywords = new HashMap<>(60);
        keywords.put("and", KW_AND);
        keywords.put("less", KW_LESS);
        keywords.put("more", KW_MORE);
        keywords.put("or", KW_OR);
        keywords.put("than", KW_THAN);
        String[] NUMBERS = new String[] { "zero",
                "one",
                "two",
                "three",
                "four",
                "five",
                "six",
                "seven",
                "eight",
                "nine",
                "ten",
                "eleven",
                "twelve",
                "thirteen",
                "fourteen",
                "fifteen",
                "sixteen",
                "seventeen",
                "eighteen",
                "nineteen",
                "twenty" };
        for (int i = 0; i < NUMBERS.length; i++) {
            keywords.put(NUMBERS[i], i);
        }
    }

    char[] s;
    int pos, prevPos;
    int intValue;
    String lastError;

    public IntRangeParser2(String str) {
        s = str != null ? str.toCharArray() : new char[0];
        pos = prevPos = 0;
        lastError = null;
    }

    private int parseNumber() {
        final int n = s.length;
        boolean negative = false;
        while (pos < n && Character.isSpaceChar((s[pos])))
            pos++;
        if (s[pos] == '+') {
            pos++;
            while (pos < n && Character.isSpaceChar((s[pos])))
                pos++;
        } else if (s[pos] == '-') {
            negative = true;
            pos++;
            while (pos < n && Character.isSpaceChar((s[pos])))
                pos++;
        }

        final int minLimit = negative ? Integer.MIN_VALUE : -Integer.MAX_VALUE, minLimit10 = minLimit / 10;
        int result = 0;
        int pos0 = pos;
        for (; pos < n; pos++) {
            char ch = s[pos];
            if (ch == ',')
                continue; // thousands separator
            if (ch < '0' || ch > '9') {
                if (pos == pos0) {
                    error("Unexpected symbol in the number");
                    return ILLEGAL;
                }
                break;
            }
            int digit = ch - '0';
            if (result < minLimit10) {
                error(ERROR_OUT_RANGE);
                return ILLEGAL;
            }
            result *= 10;
            if (result < minLimit + digit) {
                error(ERROR_OUT_RANGE);
                return ILLEGAL;
            }
            result -= digit;
        }
        while (pos < n && Character.isSpaceChar((s[pos])))
            pos++;
        pos0 = pos;
        while (pos < n && Character.isLetterOrDigit(s[pos]))
            pos++;
        if (pos != pos0 + 1)
            pos = pos0;
        else {
            switch (s[pos0]) {
                case 'K':
                    if (result < minLimit / 1000) {
                        error(ERROR_OUT_RANGE);
                        return ILLEGAL;
                    }
                    result *= 1000;
                    break;
                case 'M':
                    if (result < minLimit / 1000000) {
                        error(ERROR_OUT_RANGE);
                        return ILLEGAL;
                    }
                    result *= 1000000;
                    break;
                case 'B':
                    if (result < minLimit / 1000000000) {
                        error(ERROR_OUT_RANGE);
                        return ILLEGAL;
                    }
                    result *= 1000000000;
                    break;
                default:
                    pos = pos0;
            }
        }
        intValue = negative ? result : -result;
        return INT_VALUE;
    }

    private int nextToken(boolean allowPlusMinusAsRange) {
        final int n = s.length;
        prevPos = pos;
        while (pos < n && Character.isSpaceChar((s[pos])))
            pos++;
        if (pos >= n)
            return EOS;
        switch (s[pos]) {
            case '$':
                pos++;
                parseNumber();
                return INT_VALUE;
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return parseNumber(); // TODO optimize it little bit
            case '-':
                if (allowPlusMinusAsRange) {
                    pos++;
                    return RANGE;
                }
                return parseNumber();
            case '+':
                return allowPlusMinusAsRange ? s[pos++] : parseNumber();
            case '[':
            case '(':
            case ']':
            case ')':
                return s[pos++];
            case '…':
                pos++;
                return RANGE_EXCLUSIVE;
            case ';':
                pos++;
                return RANGE;
            case '.':
                if (++pos < n && s[pos] == '.') {
                    if (++pos < n && s[pos] == '.') {
                        pos++;
                        return RANGE_EXCLUSIVE;
                    }
                    return RANGE;
                } else {
                    return ILLEGAL;
                }
            case '>':
                if (pos + 1 < n && s[pos + 1] == '=') {
                    pos += 2;
                    return GE;
                }
                return s[pos++];
            case '<':
                if (pos + 1 < n && s[pos + 1] == '=') {
                    pos += 2;
                    return LE;
                }
                return s[pos++];
            default:
                int from = pos;
                while (pos < n && Character.isLetterOrDigit(s[pos]))
                    pos++;
                if (pos == from)
                    return ILLEGAL;
                String id = new String(s, from, pos - from).toLowerCase();
                final Integer keyword = keywords.get(id);
                if (keyword == null)
                    return ID;
                intValue = keyword;
                return keyword < KW_AND ? INT_VALUE : KEYWORD;
        }
    }

    public IntRange parse() {
        int low = Integer.MIN_VALUE, hi = Integer.MAX_VALUE;
        boolean atLeastOnePartParsed = false;
        while (true) {
            int token;
            switch (token = nextToken(false)) {
                case EOS:
                    if (!atLeastOnePartParsed)
                        return error("Integer range expected");
                    return newRange(low, hi);
                case '>':
                case '<':
                case GE:
                case LE:
                    if (nextToken(false) != INT_VALUE)
                        return error("Integer number expected");
                    switch (token) {
                        case '>':
                            low = max(low, intValue + 1);
                            break;
                        case '<':
                            hi = min(hi, intValue - 1);
                            break;
                        case GE:
                            low = max(low, intValue);
                            break;
                        case LE:
                            hi = min(hi, intValue);
                            break;
                    }
                    switch (nextToken(false)) {
                        case EOS:
                            return newRange(low, hi);
                        case KEYWORD:
                            if (intValue == KW_AND)
                                break;
                        default:
                            pos = prevPos; // return error("\"and\" or end of input expected");
                    }
                    break;
                case INT_VALUE:
                    int number = intValue;
                    switch (nextToken(true)) {
                        case EOS:
                            return newRange(intValue, intValue); // TODO
                        case '+':
                            low = max(intValue, low);
                            switch (nextToken(false)) {
                                case EOS:
                                    return newRange(low, hi);
                                case KEYWORD:
                                    if (intValue == KW_AND) {
                                        atLeastOnePartParsed = true;
                                        continue;
                                    }
                                default:
                                    return error("Unexpected input");
                            }
                        case '>':
                            hi = min(intValue - 1, hi);
                            break;
                        case '<':
                            low = max(intValue + 1, low);
                            break;
                        case GE:
                            low = max(intValue, low);
                            break;
                        case LE:
                            hi = min(intValue, hi);
                            break;
                        case RANGE:
                            if (nextToken(false) != INT_VALUE)
                                return error("Unexpected input");
                            low = max(number, low);
                            hi = min(intValue, hi);
                            break;
                        case RANGE_EXCLUSIVE:
                            if (nextToken(false) != INT_VALUE)
                                return error("Expected number");
                            low = max(number + 1, low);
                            hi = min(intValue - 1, hi);
                            break;
                        case KEYWORD:
                            switch (intValue) {
                                case KW_AND: // NUMBER and more
                                    token = nextToken(false);
                                    if (token == EOS)
                                        return error("Unexpected end of input. Is it unfinished \"and more\"?");
                                    if (token != KEYWORD || intValue != KW_MORE)
                                        return error("Unexpected input. Is it unfinished \"and more\"?");
                                    low = max(low, number);
                                    break;
                                case KW_OR: // NUMBER or less
                                    token = nextToken(false);
                                    if (token == EOS)
                                        return error("Unexpected end of input. Is it unfinished \"or less\"?");
                                    if (token != KEYWORD || intValue != KW_LESS)
                                        return error("Unexpected input. Is it unfinished \"or less\"?");
                                    hi = min(hi, number);
                                    break;
                                default:
                                    return error("Unexpected keyword");
                            }
                            break;
                        default:
                            return error("Unexpected input. Expected >,<,<=,>=, \"and more\", \"or less\"");
                    }
                    switch (nextToken(false)) {
                        case EOS:
                            return newRange(low, hi);
                        case KEYWORD:
                            if (intValue == KW_AND)
                                break;
                        default:
                            pos = prevPos; // return error("Unexpected input");
                    }
                    break;
                case KEYWORD:
                    switch (intValue) {
                        case KW_MORE:
                            token = nextToken(false);
                            if (token == EOS)
                                return error("Unexpected end of input. Should it be \"more than <NUMBER>\"?");
                            if (token != KEYWORD || intValue != KW_THAN)
                                return error("Unexpected input. Should it be \"more than <NUMBER>\"?");
                            if (nextToken(false) != INT_VALUE)
                                return error(ERROR_NOT_NUMBER);
                            low = max(low, intValue + 1);
                            break;
                        case KW_LESS:
                            token = nextToken(false);
                            if (token == EOS)
                                return error("Unexpected end of input. Should it be \"less than <NUMBER>\"?");
                            if (token != KEYWORD || intValue != KW_THAN)
                                return error("Unexpected input. Should it be \"less than <NUMBER>\"?");
                            if (nextToken(false) != INT_VALUE)
                                return error(ERROR_NOT_NUMBER);
                            hi = min(hi, intValue - 1);
                            break;
                        default:
                            return error("Unexpected keyword");
                    }
                    if (nextToken(false) != KEYWORD || intValue != KW_AND)
                        pos = prevPos;
                    break;
                case '[':
                case '(':
                    if (nextToken(false) != INT_VALUE)
                        return error(ERROR_NOT_NUMBER);
                    int c = token == '[' ? intValue : intValue + 1;
                    if (nextToken(true) != RANGE)
                        return error("Range delimiter \";\",\"-\", \"..\", expected");
                    if (nextToken(false) != INT_VALUE)
                        return error(ERROR_NOT_NUMBER);
                    int d = intValue;
                    switch (nextToken(false)) {
                        case ')':
                            d--;
                            break;
                        case ']':
                            break;
                        default:
                            return error("\")\" or \"]\" expected");
                    }
                    return newRange(max(low, c), min(hi, d));
                case ILLEGAL:
                    return null;
                case ID:
                    return error("Unexpected identifier");
                default:
                    return error("Unexpected input");
            }
            atLeastOnePartParsed = true;
        }

    }

    private IntRange error(String errorMessage) {
        lastError = errorMessage;
        return null;
    }

    private IntRange newRange(int low, int hi) {
        if (low > hi) {
            return error("The upper bound '" + hi + "' must be more or equal than the lower bound '" + low + "'.");
        }
        return new IntRange(low, hi);
    }

    public static IntRange parse(String str) {
        return new IntRangeParser2(str).parse();
    }

}
