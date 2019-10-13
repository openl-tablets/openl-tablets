package org.openl.rules.helpers;

import java.util.Objects;

import org.openl.rules.helpers.ARangeParser.ParseStruct;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;

public class CharRange extends IntRange {

    public CharRange(char min, char max) {
        super(min, max);
    }

    public CharRange(char c) {
        super(c);
    }

    public CharRange(String range) {
        super(0, 0);
        Objects.requireNonNull(range, "CharRange value can't be null.");

        ParseStruct<Character> parsed = CharRangeParser.getInstance().parse(range);
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

    private String printChar(long ch) {
        return isPrintable(ch) ? String.valueOf((char) ch) : "'u" + Integer.toHexString((char) ch) + "'";
    }

    private boolean isPrintable(long ch) {
        if (Character.isWhitespace((char) ch) || Character.isISOControl((char) ch)) {
            return false;
        }

        if (ch < 255) {
            return true;
        }
        return Character.isUnicodeIdentifierPart((char) ch);
    }

    public static CharRange autocast(char x, CharRange y) {
        return new CharRange(x);
    }

}
