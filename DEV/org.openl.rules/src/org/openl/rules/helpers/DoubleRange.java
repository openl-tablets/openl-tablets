package org.openl.rules.helpers;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.meta.*;
import org.openl.util.RangeWithBounds;
import org.openl.util.RangeWithBounds.BoundType;

/**
 * The <code>DoubleRange</code> class stores range of floats. Examples : "1.2-3", "2 .. 4", "123.456 ... 1000.00001"
 * (Important: using of ".." and "..." requires spaces between numbers and separator).
 */
@XmlRootElement
public class DoubleRange implements INumberRange {
    private static final int TO_DOUBLE_RANGE_CAST_DISTANCE = CastFactory.AFTER_FITST_WAVE_CASTS_DISTANCE + 8;
    private double lowerBound;
    private double upperBound;

    private BoundType lowerBoundType;
    private BoundType upperBoundType;

    public DoubleRange(double range) {
        this(range, range);
    }

    public DoubleRange(double lowerBound, double upperBound) {
        this(lowerBound, upperBound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    public DoubleRange(double lowerBound, double upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        if (lowerBound > upperBound) {
            throw new RuntimeException(String.format("%s must be more or equal than %s.", upperBound, lowerBound));
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
    }

    public DoubleRange() {
        lowerBound = 0;
        upperBound = 0;
    }

    public DoubleRange(String range) {
        RangeWithBounds res = getRangeWithBounds(range);

        lowerBound = res.getMin().doubleValue();
        lowerBoundType = res.getLeftBoundType();
        upperBound = res.getMax().doubleValue();
        upperBoundType = res.getRightBoundType();

        if (isTruncated(res.getMin(), lowerBound)) {
            // For example, is converted from BigDecimal to Double
            throw new IllegalArgumentException("lowerBound value is truncated");
        }
        if (isTruncated(res.getMax(), upperBound)) {
            // For example, is converted from BigDecimal to Double
            throw new IllegalArgumentException("upperBound value is truncated");
        }
    }

    public static RangeWithBounds getRangeWithBounds(String range) {
        return DoubleRangeParser.getInstance().parse(range);
    }

    /**
     * Returns true if converted value is truncated
     *
     * @param from converting number
     * @param to converted double value
     * @return true if converted value is truncated
     */
    protected static boolean isTruncated(Number from, double to) {
        return from instanceof BigDecimal && Double.isInfinite(to);
    }

    /**
     * Compares lower bounds.
     *
     * @param range the DoubleRange to be compared
     * @return a negative integer, zero, or a positive integer as lower bound of this range is less than, equal to, or
     *         greater than the lower bound of specified range.
     */
    public int compareLowerBound(DoubleRange range) {
        if (lowerBound < range.lowerBound) {
            return -1;
        } else if (lowerBound == range.lowerBound) {
            if (lowerBoundType == BoundType.INCLUDING && range.lowerBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (lowerBoundType == range.lowerBoundType) {
                return 0;
            }
        }
        return 1;
    }

    /**
     * Compares upper bounds.
     *
     * @param range the DoubleRange to be compared
     * @return a negative integer, zero, or a positive integer as upper bound of this range is less than, equal to, or
     *         greater than the upper bound of specified range.
     */
    public int compareUpperBound(DoubleRange range) {
        if (upperBound < range.upperBound) {
            return -1;
        } else if (upperBound == range.upperBound) {
            if (upperBoundType == BoundType.INCLUDING && range.upperBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (upperBoundType == range.upperBoundType) {
                return 0;
            }
        }
        return 1;
    }

    public boolean contains(double x) {
        if (lowerBound < x && x < upperBound) {
            return true;
        } else if (x == lowerBound && lowerBoundType == BoundType.INCLUDING) {
            return true;
        } else if (x == upperBound && upperBoundType == BoundType.INCLUDING) {
            return true;
        }
        return false;
    }

    public boolean contains(DoubleValue value) {
        if (value == null) {
            return false;
        }
        return contains(value.doubleValue());
    }

    public boolean contains(DoubleRange range) {
        return compareLowerBound(range) <= 0 && compareUpperBound(range) >= 0;
    }

    public boolean contains(BigDecimalValue value) {
        if (value == null) {
            return false;
        }
        return contains(value.getValue().doubleValue());
    }

    @Override
    public boolean containsNumber(Number n) {
        return n != null && contains(n.doubleValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DoubleRange)) {
            return false;
        }
        DoubleRange that = (DoubleRange) o;
        return Double.compare(that.lowerBound, lowerBound) == 0 && Double.compare(that.upperBound,
            upperBound) == 0 && lowerBoundType == that.lowerBoundType && upperBoundType == that.upperBoundType;
    }

    /**
     * @return Returns the lowerBound.
     */
    public double getLowerBound() {
        return lowerBound;
    }

    /**
     * @return Returns the upperBound.
     */
    public double getUpperBound() {
        return upperBound;
    }

    public DoubleRange intersect(DoubleRange range) {
        int lowerBoundComaring = compareLowerBound(range);
        int upperBoundComaring = compareUpperBound(range);

        double lowerBound = lowerBoundComaring > 0 ? this.lowerBound : range.lowerBound;
        BoundType lowerBoundType = lowerBoundComaring > 0 ? this.lowerBoundType : range.lowerBoundType;
        double upperBound = upperBoundComaring < 0 ? this.upperBound : range.upperBound;
        BoundType upperBoundType = upperBoundComaring < 0 ? this.upperBoundType : range.upperBoundType;
        return lowerBound > upperBound ? null : new DoubleRange(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    /**
     * @param lowerBound The lowerBound to set.
     */
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @param upperBound The upperBound to set.
     */
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public void setLowerBoundType(BoundType lowerBoundType) {
        this.lowerBoundType = lowerBoundType;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public void setUpperBoundType(BoundType upperBoundType) {
        this.upperBoundType = upperBoundType;
    }

    @Override
    public String toString() {
        if (lowerBound == Double.NEGATIVE_INFINITY) {
            return (upperBoundType == BoundType.INCLUDING ? "<=" : "<") + upperBound;
        } else if (upperBound == Double.POSITIVE_INFINITY) {
            return (lowerBoundType == BoundType.INCLUDING ? ">=" : ">") + lowerBound;
        }

        StringBuilder builder = new StringBuilder();
        if (lowerBoundType == BoundType.INCLUDING) {
            builder.append('[');
        } else {
            builder.append('(');
        }
        builder.append(lowerBound).append("; ").append(upperBound);
        if (upperBoundType == BoundType.INCLUDING) {
            builder.append(']');
        } else {
            builder.append(')');
        }
        return builder.toString();
    }

    public static DoubleRange autocast(byte x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(byte x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(short x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(short x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(int x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(int x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(long x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(long x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(float x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(float x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(double x, DoubleRange y) {
        return new DoubleRange(x);
    }

    public static int distance(double x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(BigInteger x, DoubleRange y) {
        return new DoubleRange(x.doubleValue());
    }

    public static int distance(BigInteger x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(BigDecimal x, DoubleRange y) {
        return new DoubleRange(x.doubleValue());
    }

    public static int distance(BigDecimal x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(ByteValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(ByteValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(ShortValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(ShortValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(IntValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(IntValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(LongValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(LongValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(FloatValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(FloatValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(DoubleValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(DoubleValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(BigIntegerValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(BigIntegerValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange cast(BigDecimalValue x, DoubleRange y) {
        return new DoubleRange(x.intValue());
    }

    public static int distance(BigDecimalValue x, DoubleRange y) {
        return TO_DOUBLE_RANGE_CAST_DISTANCE;
    }

    public static DoubleRange autocast(IntRange x, DoubleRange y) {
        return new DoubleRange(x.getMin(), x.getMax());
    }
}
