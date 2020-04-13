package org.openl.rules.helpers;

import java.util.Objects;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.helpers.ARangeParser.ParseStruct;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.util.StringUtils;

public class CharRange extends IntRange {

    private static final int TO_CHAR_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;

    public CharRange(char min, char max) {
        super(min, max);
    }

    public CharRange(char c) {
        super(c);
    }

    public CharRange(String range) {
        super(0, 0);
        Objects.requireNonNull(range, "CharRange value cannot be null");

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
            throw new RuntimeException(String.format("%s must be more or equal than %s", parsed.max, parsed.min));
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
        if (StringUtils.isSpaceOrControl((char) ch)) {
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

    public static int distance(char x, CharRange y) {
        return TO_CHAR_RANGE_CAST_DISTANCE;
    }

}
