/*
 * Created on Jul 7, 2005
 */
package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.meta.*;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * The <code>IntRange</code> class stores range of integers. Examples : "1-3", "2 .. 4", "123 ... 1000" (Important:
 * using of ".." and "..." requires spaces between numbers and separator).
 */
@XmlRootElement
public class IntRange implements INumberRange {

    protected long min;
    protected long max;

    /**
     * Constructor for <code>IntRange</code> with provided <code>min</code> and <code>max</code> values.
     */
    public IntRange(long min, long max) {
        if (min > max) {
            throw new RuntimeException(max + " must be more or equal than " + min);
        }
        this.min = min;
        this.max = max;
    }

    public IntRange(long number) {
        this(number, number);
    }

    public boolean contains(IntValue value) {
        return contains(value.longValue());
    }

    public boolean contains(LongValue value) {
        return contains(value.longValue());
    }

    public IntRange() {
        this(0, 0);
    }

    public boolean contains(long value) {
        return min <= value && value <= max;
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    @Override
    public boolean containsNumber(Number n) {
        return n != null && contains(n.longValue());
    }

    /**
     * Constructor for <code>IntRange</code>. Tries to parse range text with variety of formats. Supported range
     * formats: "<min number> - <max number>" or "[<, <=, >, >=]<number>" or "<number>+" Also numbers can be enhanced
     * with $ sign and K,M,B, e.g. $1K = 1000 Any symbols at the end are allowed to support expressions like ">=2
     * barrels", "6-8 km^2"
     */
    public IntRange(String range) {
        this(0, 0);
        RangeWithBounds res = getRangeWithBounds(range);

        min = res.getMin().longValue();
        if (!res.getMin().equals(min)) {
            // For example, is converted from Long to Integer
            throw new IllegalArgumentException("Min value is out of int values range.");
        }
        if (res.getLeftBoundType() == BoundType.EXCLUDING) {
            min++;
        }

        max = res.getMax().longValue();
        if (!res.getMax().equals(max)) {
            // For example, is converted from Long to Integer
            throw new IllegalArgumentException("Max value is out of int values range.");
        }
        if (res.getRightBoundType() == BoundType.EXCLUDING) {
            max--;
        }
        if (min > max) {
            throw new RuntimeException(max + " must be more or equal than " + min);
        }
    }

    public static RangeWithBounds getRangeWithBounds(String range) {
        return IntRangeParser.getInstance().parse(range);
    }

    public static IntRange autocast(byte x, IntRange y) {
        return new IntRange(x);
    }

    public static IntRange autocast(short x, IntRange y) {
        return new IntRange(x);
    }

    public static IntRange autocast(int x, IntRange y) {
        return new IntRange(x);
    }

    public static IntRange autocast(long x, IntRange y) {
        return new IntRange(x);
    }

    public static IntRange cast(float x, IntRange y) {
        return new IntRange((long) x);
    }

    public static IntRange cast(double x, IntRange y) {
        return new IntRange((long) x);
    }

    public static IntRange cast(BigInteger x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static IntRange cast(BigDecimal x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static IntRange cast(ByteValue x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static IntRange cast(ShortValue x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static IntRange cast(IntValue x, IntRange y) {
        return new IntRange(x.longValue());
    }

    public static IntRange cast(LongValue x, IntRange y) {
        return new IntRange(x.intValue());
    }

    public static IntRange cast(FloatValue x, IntRange y) {
        return new IntRange(x.intValue());
    }

    public static IntRange cast(DoubleValue x, IntRange y) {
        return new IntRange(x.intValue());
    }

    public static IntRange cast(BigIntegerValue x, IntRange y) {
        return new IntRange(x.intValue());
    }

    public static IntRange cast(BigDecimalValue x, IntRange y) {
        return new IntRange(x.intValue());
    }

    @Override
    public String toString() {
        if (min == Integer.MIN_VALUE) {
            return "<=" + max;
        } else if (max == Integer.MAX_VALUE) {
            return ">=" + min;
        }

        return "[" + min + ".." + max + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (max ^ max >>> 32);
        result = prime * result + (int) (min ^ min >>> 32);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        IntRange other = (IntRange) obj;
        if (max != other.max) {
            return false;
        }
        if (min != other.min) {
            return false;
        }
        return true;
    }
}
