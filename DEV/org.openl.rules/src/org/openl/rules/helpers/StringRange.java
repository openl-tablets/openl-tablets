package org.openl.rules.helpers;

import java.util.Objects;

import javax.xml.bind.annotation.XmlRootElement;

import org.openl.meta.StringValue;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.ARangeParser.ParseStruct;
import org.openl.util.StringUtils;

@XmlRootElement
public class StringRange {

    private final String lowerBound;
    private final String upperBound;

    private final BoundType lowerBoundType;
    private final BoundType upperBoundType;

    StringRange(String lowerBound, String upperBound) {
        this(lowerBound, upperBound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    StringRange(String lowerBound, String upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
        validate();
    }

    @SuppressWarnings("WeakerAccess")
    public StringRange(String source) {
        ParseStruct<String> range = StringRangeParser.getInstance().parse(source);
        this.lowerBound = range.min;
        this.lowerBoundType = range.leftBoundType;
        this.upperBound = range.max;
        this.upperBoundType = range.rightBoundType;
        validate();
    }

    private void validate() {
        if (lowerBoundType == null || upperBoundType == null) {
            throw new IllegalStateException("Bound types must be initialized!");
        }
        if (lowerBound == null || upperBound == null) {
            throw new IllegalStateException("All bounds must be initialized!");
        }
        if (lowerBound.compareTo(upperBound) > 0) {
            throw new IllegalStateException("Left bound must be lower than right!");
        }
    }

    public String getLowerBound() {
        return lowerBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public String getUpperBound() {
        return upperBound;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public boolean contains(String s) {
        if (s == null) {
            return false;
        }
        int lowerComparison = lowerBound.compareTo(s);
        int upperComparison = upperBound.compareTo(s);
        if (lowerComparison < 0 && upperComparison > 0) {
            return true;
        } else if (lowerComparison == 0 && lowerBoundType == BoundType.INCLUDING) {
            return true;
        } else if (upperComparison == 0 && upperBoundType == BoundType.INCLUDING) {
            return true;
        }
        return false;
    }

    public boolean contains(CharSequence s) {
        return contains(s == null ? null : s.toString());
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
        return Objects.equals(lowerBound, that.lowerBound) && Objects.equals(upperBound,
            that.upperBound) && lowerBoundType == that.lowerBoundType && upperBoundType == that.upperBoundType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (StringRangeParser.MIN_VALUE.equals(lowerBound)) {
            sb.append(upperBoundType == BoundType.INCLUDING ? "<= " : "< ");
            sb.append(upperBound);
        } else if (StringRangeParser.MAX_VALUE.equals(upperBound)) {
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

    //CAST METHODS
    public static StringRange cast(char[] x, StringRange y) {
        return new StringRange(String.valueOf(x));
    }

    public static StringRange cast(StringValue x, StringRange y) {
        return new StringRange(x.getValue());
    }
    //END

}
