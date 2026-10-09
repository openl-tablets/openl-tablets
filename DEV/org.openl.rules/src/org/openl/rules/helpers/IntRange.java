package org.openl.rules.helpers;

import java.beans.Transient;
import java.math.BigDecimal;
import java.math.BigInteger;
import jakarta.xml.bind.annotation.XmlRootElement;

import lombok.Getter;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.range.Range;
import org.openl.rules.range.RangeParser;

/**
 * The <code>IntRange</code> class stores range of integers. Examples : "1-3", "2 .. 4", "123 ... 1000" (Important:
 * using of ".." and "..." requires spaces between numbers and separator).
 */
// Range.equals already compares the bounds this range keeps, reading them through getType, getLeft and getRight.
// A parameter that is not read selects the overload by its type: the engine resolves casts and distances by signature.
@SuppressWarnings({"java:S1172", "java:S2160"})
@XmlRootElement
public class IntRange extends Range<Long> implements INumberRange {
    private static final int TO_INT_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;

    @Getter
    protected long min;
    @Getter
    protected long max;
    @Getter(onMethod_ = {@Transient})
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

    @Override
    public boolean contains(Number n) {
        return n != null && contains(n.longValue());
    }

    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with variety of formats. Supported range
     * formats: "<min number> - <max number>" or "[<, <=, >, >=]<number>" or "<number>+".
     *
     * <p>A bound is a whole number. It may start with a {@code $} sign, may group its thousands with commas and may end
     * with a {@code K}, {@code M} or {@code B} multiplier, so {@code $1,500} is 1500 and {@code 10K} is 10000. A comma
     * must separate groups of exactly three digits.
     */
    public IntRange(String range) {
        Type rangeType;
        try {
            rangeType = parseBounds(range);
        } catch (RuntimeException ex) {
            rangeType = parseVerbalBounds(range, ex);
        }
        this.type = rangeType;
        validate();
    }

    private Type parseBounds(String range) {
        var parser = parse(range);
        if (parser == null) {
            this.min = NumberBound.of(range.trim()).toLong();
            this.max = this.min;
            return Type.DEGENERATE;
        }
        return applyBounds(parser);
    }

    /**
     * Parses a range with bounds written in words, like "less than 5" or "5 and more".
     *
     * @param failure the failure to report when the range cannot be parsed this way either
     */
    private Type parseVerbalBounds(String range, RuntimeException failure) {
        if (!range.contains("less") && !range.contains("more")) {
            throw failure;
        }
        try {
            return applyBounds(parse(replaceVerbalBounds(range)));
        } catch (Exception ignore) {
            throw failure;
        }
    }

    private Type applyBounds(RangeParser parser) {
        var rangeType = parser.getType();
        var left = parser.getLeft();
        var right = parser.getRight();
        this.min = left == null ? Long.MIN_VALUE : NumberBound.of(left).toLong();
        this.max = right == null ? Long.MAX_VALUE : NumberBound.of(right).toLong();
        return rangeType;
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
}
