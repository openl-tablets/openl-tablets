package org.openl.rules.helpers;

import java.beans.Transient;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.range.Range;

/**
 * The <code>IntRange</code> class stores range of integers. Examples : "1-3", "2 .. 4", "123 ... 1000" (Important:
 * using of ".." and "..." requires spaces between numbers and separator).
 */
@XmlRootElement
public class IntRange extends Range<Long> implements INumberRange {
    private static final int TO_INT_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;

    protected long min;
    protected long max;
    protected final Type type;

    /**
     * Constructor for <code>IntRange</code> with provided <code>min</code> and <code>max</code> values.
     */
    public IntRange(long min, long max) {
        this.min = min;
        this.max = max;
        if (max == Long.MAX_VALUE) {
            type = Type.LEFT_CLOSED;
        } else if (min == Long.MIN_VALUE) {
            type = Type.RIGHT_CLOSED;
        } else {
            type = Type.CLOSED;
        }
        validate();
    }

    public IntRange(long number) {
        this.min = number;
        this.max = number;
        this.type = Type.DEGENERATE;
    }

    public IntRange() {
        this.min = 0;
        this.max = 0;
        this.type = Type.DEGENERATE;
    }

    public boolean contains(BigInteger value) {
        if (value == null) {
            return false;
        }
        try {
            return contains(value.longValueExact());
        } catch (ArithmeticException e) {
            return false;
        }
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    @Override
    public boolean contains(Number n) {
        return n != null && contains(n.longValue());
    }

    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with variety of formats. Supported range
     * formats: "<min number> - <max number>" or "[<, <=, >, >=]<number>" or "<number>+" Also numbers can be enhanced
     * with $ sign and K,M,B, e.g. $1K = 1000 Any symbols at the end are allowed to support expressions like ">=2
     * barrels", "6-8 km^2"
     */
    public IntRange(String range) {
        Type type;
        try {
            var parser = parse(range);
            if (parser == null) {
                type = Type.DEGENERATE;
                this.min = convertToLong(range.trim());
                this.max = this.min;
            } else {
                type = parser.getType();
                var left = parser.getLeft();
                var right = parser.getRight();
                this.min = left == null ? Long.MIN_VALUE : convertToLong(left);
                this.max = right == null ? Long.MAX_VALUE : convertToLong(right);
            }
        } catch (RuntimeException ex) {
            try {
                if (range.contains("less") || range.contains("more")) {
                    range = range
                            .replaceAll("less\\s+than", "<")
                            .replaceAll("more\\s+than", ">")
                            .replaceAll("(\\S+)\\s+or\\s+less", "<=$1")
                            .replaceAll("(\\S+)\\s+and\\s+more", ">=$1");
                    var parser = parse(range);
                    type = parser.getType();
                    String left = parser.getLeft();
                    String right = parser.getRight();
                    min = left == null ? Long.MIN_VALUE : convertToLong(left);
                    max = right == null ? Long.MAX_VALUE : convertToLong(right);
                } else {
                    throw ex;
                }
            } catch (Exception ignore) {
                throw ex;
            }
        }
        this.type = type;
        validate();
    }

    @Override
    @Transient
    public Type getType() {
        return type;
    }

    @Override
    protected Long getLeft() {
        return min;
    }

    @Override
    protected Long getRight() {
        return max;
    }

    @Override
    protected int compare(Long left, Long right) {
        return Long.compare(left, right);
    }

    public static IntRange autocast(byte x, IntRange y) {
        return new IntRange(x);
    }

    public static int distance(byte x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange autocast(short x, IntRange y) {
        return new IntRange(x);
    }

    public static int distance(short x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange autocast(int x, IntRange y) {
        return new IntRange(x);
    }

    public static int distance(int x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange autocast(long x, IntRange y) {
        return new IntRange(x);
    }

    public static int distance(long x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange cast(float x, IntRange y) {
        return new IntRange((long) x);
    }

    public static int distance(float x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange cast(double x, IntRange y) {
        return new IntRange((long) x);
    }

    public static int distance(double x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange cast(BigInteger x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static int distance(BigInteger x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    public static IntRange cast(BigDecimal x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static int distance(BigDecimal x, IntRange y) {
        return TO_INT_RANGE_CAST_DISTANCE;
    }

    private static long convertToLong(String text) {
        long multiplier = 1;
        int start = 0;
        if (text.startsWith("$")) {
            start++;
        }
        if (text.charAt(start) == ',') {
            // special case, when comma as a group separator is in the beginning.
            throw new NumberFormatException("For input string: \"" + text + "\"");
        }
        int end = text.length();
        switch (text.charAt(end - 1)) {
            case 'B':
                multiplier *= 1000;
            case 'M':
                multiplier *= 1000;
            case 'K':
                multiplier *= 1000;
                end--;
                break;
        }
        if (!Character.isDigit(text.charAt(end - 1))) {
            // special case, when comma as a group separator is in the ending.
            throw new NumberFormatException("For input string: \"" + text + "\"");
        }
        text = text.substring(start, end).replace(",", "");
        long value = Long.parseLong(text);
        return Math.multiplyExact(value, multiplier);
    }
}
