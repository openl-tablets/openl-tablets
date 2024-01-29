package org.openl.rules.helpers;

import java.beans.Transient;
import java.util.Comparator;
import java.util.Objects;
import javax.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.range.Range;
import org.openl.util.StringUtils;

@XmlRootElement
public class CharRange extends Range<Character> {

    private static final int TO_CHAR_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;
    protected final Character min;
    protected final Character max;
    protected final Type type;
    public CharRange(char min, char max) {
        this.min = min;
        this.max = max;
        if (max == Character.MAX_VALUE) {
            type = Type.LEFT_CLOSED;
        } else if (min == Character.MIN_VALUE) {
            type = Type.RIGHT_CLOSED;
        } else {
            type = Type.CLOSED;
        }
        validate();
    }

    public CharRange(char c) {
        this.min = c;
        this.max = c;
        this.type = Type.DEGENERATE;
    }

    public CharRange(String range) {
        Objects.requireNonNull(range, "CharRange value cannot be null");
        var parser = parse(range);
        if (parser == null) {
            this.type = Type.DEGENERATE;
            this.min = convertToChar(range.trim());
            this.max = this.min;
        } else {
            this.type = parser.getType();
            var left = parser.getLeft();
            var right = parser.getRight();
            this.min = left == null ? Character.MIN_VALUE : convertToChar(left);
            this.max = right == null ? Character.MAX_VALUE : convertToChar(right);
            validate();
        }
    }

    @Override
    public boolean contains(Character value) {
        return super.contains(value);
    }

    @Override
    @Transient
    public Type getType() {
        return type;
    }

    @Override
    protected Character getLeft() {
        return min;
    }

    @Override
    protected Character getRight() {
        return max;
    }

    public Character getMin() {
        return min;
    }

    public Character getMax() {
        return max;
    }

    @Override
    protected int compare(Character left, Character right) {
        return Comparator.<Character>nullsLast(Comparator.naturalOrder()).compare(left, right);
    }

    @Override
    protected void format(StringBuilder sb, Character ch) {
        boolean printable = !StringUtils.isSpaceOrControl(ch) && (ch < 255 || Character.isUnicodeIdentifierPart(ch));

        sb.append(printable ? String.valueOf(ch) : String.format("\\u%04x", (int) ch));
    }

    public static CharRange autocast(char x, CharRange y) {
        return new CharRange(x);
    }

    public static int distance(char x, CharRange y) {
        return TO_CHAR_RANGE_CAST_DISTANCE;
    }


    private static Character convertToChar(String text) {
        if (text == null) {
            return null;
        }
        if (text.length() != 1) {
            throw new IllegalArgumentException("Only one character can be defined");
        }
        return text.charAt(0);
    }
}
