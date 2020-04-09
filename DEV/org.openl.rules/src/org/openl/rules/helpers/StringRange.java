package org.openl.rules.helpers;

import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.NumericComparableString;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.meta.StringValue;
import org.openl.rules.helpers.ARangeParser.ParseStruct;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;

@XmlRootElement
public class StringRange {

    private static final int TO_STRING_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;

    private final NumericComparableString lowerBound;
    private final NumericComparableString upperBound;

    private final BoundType lowerBoundType;
    private final BoundType upperBoundType;

    StringRange(String lowerBound, String upperBound) {
        this(lowerBound, upperBound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    StringRange(String lowerBound, String upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        this.lowerBound = NumericComparableString.valueOf(lowerBound);
        this.upperBound = NumericComparableString.valueOf(upperBound);
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
        validate();
    }

    public StringRange(String source) {
        ParseStruct<String> range = StringRangeParser.getInstance().parse(source);
        this.lowerBound = NumericComparableString.valueOf(range.min);
        this.lowerBoundType = range.leftBoundType;
        this.upperBound = NumericComparableString.valueOf(range.max);
        this.upperBoundType = range.rightBoundType;
        validate();
    }

    private void validate() {
        if (lowerBoundType == null || upperBoundType == null) {
            throw new IllegalStateException("Bound types must be initialized.");
        }
        if (lowerBound == null || upperBound == null) {
            throw new IllegalStateException("All bounds must be initialized.");
        }
        if (lowerBound.compareTo(upperBound) > 0) {
            throw new IllegalStateException("Left bound must be lower than right.");
        }
    }

    public NumericComparableString getLowerBound() {
        return lowerBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public NumericComparableString getUpperBound() {
        return upperBound;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public boolean contains(NumericComparableString s) {
        if (s == null) {
            return false;
        }
        int lowerComparison = lowerBound.compareTo(s);
        int upperComparison = upperBound.compareTo(s);
        if (lowerComparison < 0 && upperComparison > 0) {
            return true;
        } else if (lowerComparison == 0 && lowerBoundType == BoundType.INCLUDING) {
            return true;
        } else {
            return upperComparison == 0 && upperBoundType == BoundType.INCLUDING;
        }
    }

    public boolean contains(CharSequence s) {
        return contains(s == null ? null : NumericComparableString.valueOf(s.toString()));
    }

    public int compareUpperBound(StringRange range) {
        if (upperBound.compareTo(range.upperBound) < 0) {
            return -1;
        } else if (upperBound.compareTo(range.upperBound) == 0) {
            if (upperBoundType == BoundType.INCLUDING && range.upperBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (upperBoundType == range.upperBoundType) {
                return 0;
            }
        }
        return 1;
    }

    public int compareLowerBound(StringRange range) {
        if (lowerBound.compareTo(range.lowerBound) < 0) {
            return -1;
        } else if (lowerBound.compareTo(range.lowerBound) == 0) {
            if (lowerBoundType == BoundType.INCLUDING && range.lowerBoundType == BoundType.EXCLUDING) {
                return -1;
            } else if (lowerBoundType == range.lowerBoundType) {
                return 0;
            }
        }
        return 1;
    }

    public boolean contains(StringRange range) {
        return compareLowerBound(range) <= 0 && compareUpperBound(range) >= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StringRange that = (StringRange) o;
        return Objects.equals(lowerBound.getValue(), that.lowerBound.getValue()) && Objects.equals(
            upperBound.getValue(),
            that.upperBound
                .getValue()) && lowerBoundType == that.lowerBoundType && upperBoundType == that.upperBoundType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (StringRangeParser.MIN_VALUE.equals(lowerBound.getValue())) {
            sb.append(upperBoundType == BoundType.INCLUDING ? "<= " : "< ");
            sb.append(upperBound);
        } else if (StringRangeParser.MAX_VALUE.equals(upperBound.getValue())) {
            sb.append(lowerBoundType == BoundType.INCLUDING ? ">= " : "> ");
            sb.append(lowerBound);
        } else {
            sb.append(lowerBoundType == BoundType.INCLUDING ? '[' : '(');
            sb.append(lowerBound);
            sb.append("; ");
            sb.append(upperBound);
            sb.append(upperBoundType == BoundType.INCLUDING ? ']' : ')');
        }
        return sb.toString();
    }

    // AUTOCASTS
    public static StringRange autocast(String x, StringRange y) {
        return new StringRange(x);
    }

    public static int distance(String x, StringRange y) {
        return TO_STRING_RANGE_CAST_DISTANCE;
    }

    // CAST METHODS
    public static StringRange cast(char[] x, StringRange y) {
        return new StringRange(String.valueOf(x));
    }

    public static int distance(char[] x, StringRange y) {
        return TO_STRING_RANGE_CAST_DISTANCE;
    }

    public static StringRange cast(StringValue x, StringRange y) {
        return new StringRange(x.getValue());
    }

    public static int distance(StringValue x, StringRange y) {
        return TO_STRING_RANGE_CAST_DISTANCE;
    }
    // END

}
