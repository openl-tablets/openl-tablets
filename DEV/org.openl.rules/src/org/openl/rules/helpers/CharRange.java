package org.openl.rules.helpers;

import org.openl.rules.helpers.CharRangeParser.ParseStruct;
import org.openl.rules.helpers.CharRangeParser.ParseStruct.BoundType;

public class CharRange extends IntRange {

    public CharRange(char min, char max) {
        super(min, max);
    }

    public CharRange(char c) {
        super((int) c);
    }

    public CharRange(String range) {
        super(0, 0);
        if (range == null)
            throw new NullPointerException("CharRange value can not be null");

        ParseStruct parsed = CharRangeParser.getInstance().parse(range);
        min = parsed.min;
        if (parsed.leftBoundType == BoundType.EXCLUDING) {
            min += 1;
        }
        max = parsed.max;
        if (parsed.rightBoundType == BoundType.EXCLUDING) {
            max -= 1;
        }
        if (min > max) {
            throw new RuntimeException(parsed.max + " must be more or equal than " + parsed.min);
        }
    }

    @Override
    public String toString() {
        return printChar(min) + "-" + printChar(max);
    }

    private String printChar(int ch) {
        return isPrintable(ch) ? String.valueOf((char) ch) : "'u" + Integer.toHexString(ch) + "'";
    }

    private boolean isPrintable(int ch) {
        if (Character.isWhitespace(ch) || Character.isISOControl(ch))
            return false;

        if (ch < 255)
            return true;
        if (Character.isUnicodeIdentifierPart(ch))
            return true;
        return false;
    }

    public static CharRange autocast(char x, CharRange y) {
        return new CharRange(x);
    }

}
