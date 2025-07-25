package org.openl.rules.helpers;

import java.beans.Transient;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.openl.binding.impl.NumericComparableString;
import org.openl.binding.impl.NumericStringComparator;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.range.Range;

@XmlRootElement
public class StringRange extends Range<CharSequence> {

    private static final int TO_STRING_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;

    private final Type type;
    private final NumericComparableString lowerBound;
    private final NumericComparableString upperBound;

    public StringRange(String source) {
        var rangeParser = parse(source);
        if (rangeParser == null) {
            this.type = Type.DEGENERATE;
            this.lowerBound = NumericComparableString.valueOf(source.trim());
            this.upperBound = this.lowerBound;
        } else {
            this.type = rangeParser.getType();
            var left = rangeParser.getLeft();
            this.lowerBound = NumericComparableString.valueOf(left == null ? StringRangeParser.MIN_VALUE : left);
            var right = rangeParser.getRight();
            this.upperBound = NumericComparableString.valueOf(right == null ? StringRangeParser.MAX_VALUE : right);
            validate();
        }
    }

    public NumericComparableString getLowerBound() {
        return lowerBound;
    }

    public NumericComparableString getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean contains(CharSequence value) {
        return super.contains(value);
    }

    @Override
    @Transient
    public Range.Type getType() {
        return type;
    }

    @Override
    protected CharSequence getLeft() {
        return lowerBound.getValue();
    }

    @Override
    protected CharSequence getRight() {
        return upperBound.getValue();
    }

    @Override
    protected int compare(CharSequence left, CharSequence right) {
        return NumericStringComparator.INSTANCE.compare(left, right);
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
    // END

}
