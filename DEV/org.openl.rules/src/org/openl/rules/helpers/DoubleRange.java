package org.openl.rules.helpers;

import java.beans.Transient;
import java.math.BigDecimal;
import java.math.BigInteger;
import javax.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.range.Range;


/**
 * The <code>DoubleRange</code> class stores range of floats. Examples : "1.2-3", "2 .. 4", "123.456 ... 1000.00001"
 * (Important: using of ".." and "..." requires spaces between numbers and separator).
 */
@XmlRootElement
public class DoubleRange extends Range<Double> implements INumberRange {
    private static final int TO_DOUBLE_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;
    private double lowerBound;
    private double upperBound;

    private final Type type;

    public DoubleRange(double range) {
        lowerBound = range;
        upperBound = range;
        type = Type.DEGENERATE;
    }

    public DoubleRange(double lowerBound, double upperBound) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (Double.isInfinite(upperBound)) {
            type = Type.LEFT_CLOSED;
        } else if (Double.isInfinite(lowerBound)) {
            type = Type.RIGHT_CLOSED;
        } else {
            type = Type.CLOSED;
        }
        validate();
    }

    public DoubleRange(double lowerBound, double upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        if (Double.isInfinite(upperBound)) {
            type = lowerBoundType == BoundType.EXCLUDING ? Type.LEFT_OPEN : Type.LEFT_CLOSED;
        } else if (Double.isInfinite(lowerBound)) {
            type = upperBoundType == BoundType.EXCLUDING ? Type.RIGHT_OPEN : Type.RIGHT_CLOSED;
        } else if (upperBoundType == BoundType.EXCLUDING) {
            type = lowerBoundType == BoundType.EXCLUDING ? Type.OPEN : Type.CLOSED_OPEN;
        } else {
            type = lowerBoundType == BoundType.EXCLUDING ? Type.OPEN_CLOSED : Type.CLOSED;
        }
        validate();
    }

    public DoubleRange() {
        lowerBound = 0;
        upperBound = 0;
        type = Type.DEGENERATE;
    }

    public DoubleRange(String range) {
        Type type;
        try {
            var parser = parse(range);
            if (parser == null) {
                type = Type.DEGENERATE;
                this.lowerBound = convertToDouble(range.trim());
                this.upperBound = this.lowerBound;
            } else {
                type = parser.getType();
                var left = parser.getLeft();
                var right = parser.getRight();
                lowerBound = left == null ? Double.NEGATIVE_INFINITY : convertToDouble(left);
                upperBound = right == null ? Double.POSITIVE_INFINITY : convertToDouble(right);
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
                    lowerBound = left == null ? Double.NEGATIVE_INFINITY : convertToDouble(left);
                    upperBound = right == null ? Double.POSITIVE_INFINITY : convertToDouble(right);
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

    /**
     * Returns true if converted value is truncated
     *
     * @param from converting number
     * @param to   converted double value
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
     * greater than the lower bound of specified range.
     */
    @Deprecated
    public int compareLowerBound(DoubleRange range) {
        if (lowerBound < range.lowerBound) {
            return -1;
        } else if (lowerBound == range.lowerBound) {
            if (type.left != Bound.OPEN && range.type.left == Bound.OPEN) {
                return -1;
            } else if (type.left == range.type.left) {
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
     * greater than the upper bound of specified range.
     */
    @Deprecated
    public int compareUpperBound(DoubleRange range) {
        if (upperBound < range.upperBound) {
            return -1;
        } else if (upperBound == range.upperBound) {
            if (type.right != Bound.OPEN && range.type.right == Bound.OPEN) {
                return -1;
            } else if (type.right == range.type.right) {
                return 0;
            }
        }
        return 1;
    }

    @Override
    public boolean contains(Number n) {
        if (n instanceof Float) {
            // prevent precision loosing in decimal numeral system
            return contains(Double.parseDouble(n.toString()));
        }
        return n != null && contains(n.doubleValue());
    }

    @Override
    @Transient
    public Type getType() {
        return type;
    }

    @Override
    protected Double getLeft() {
        return lowerBound;
    }

    @Override
    protected Double getRight() {
        return upperBound;
    }

    @Override
    protected int compare(Double left, Double right) {
        return Double.compare(left, right);
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

    @Deprecated
    public DoubleRange intersect(DoubleRange range) {
        int lowerBoundComaring = compareLowerBound(range);
        int upperBoundComaring = compareUpperBound(range);

        double lowerBound = lowerBoundComaring > 0 ? this.lowerBound : range.lowerBound;
        BoundType lowerBoundType = lowerBoundComaring > 0 ? this.getLowerBoundType() : range.getLowerBoundType();
        double upperBound = upperBoundComaring < 0 ? this.upperBound : range.upperBound;
        BoundType upperBoundType = upperBoundComaring < 0 ? this.getUpperBoundType() : range.getUpperBoundType();
        return lowerBound > upperBound ? null : new DoubleRange(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    /**
     * @param lowerBound The lowerBound to set.
     */
    @Deprecated
    public void setLowerBound(double lowerBound) {
        this.lowerBound = lowerBound;
    }

    /**
     * @param upperBound The upperBound to set.
     */
    @Deprecated
    public void setUpperBound(double upperBound) {
        this.upperBound = upperBound;
    }

    @Deprecated
    public BoundType getLowerBoundType() {
        return type.left == Bound.OPEN ? BoundType.EXCLUDING : BoundType.INCLUDING;
    }

    @Deprecated
    public void setLowerBoundType(BoundType lowerBoundType) {

    }

    @Deprecated
    public BoundType getUpperBoundType() {
        return type.right == Bound.OPEN ? BoundType.EXCLUDING : BoundType.INCLUDING;
    }

    @Deprecated
    public void setUpperBoundType(BoundType upperBoundType) {

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
        return new DoubleRange(new BigDecimal(String.valueOf(x)).doubleValue());
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

    public static DoubleRange autocast(IntRange x, DoubleRange y) {
        return new DoubleRange(x.getMin(), x.getMax());
    }

    private static double convertToDouble(String text) {
        double multiplier = 1.0;
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
        if (!Character.isDigit(text.charAt(end - 1)) || text.indexOf('e') >= 0 || text.indexOf('E') >= 0) {
            // special case, when comma or decimal separator, or letter is in the ending.
            // These symbols are prohibited even if they are valid for Java numbers.
            throw new NumberFormatException("For input string: \"" + text + "\"");
        }
        text = text.substring(start, end).replace(",", "");
        double value = Double.parseDouble(text);
        return value * multiplier;
    }

}
