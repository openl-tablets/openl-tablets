package org.openl.rules.helpers;

import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.helpers.ARangeParser.ParseStruct;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class DateRange {

    private final long lowerBound;
    private final long upperBound;

    private final BoundType lowerBoundType;
    private final BoundType upperBoundType;

    @SuppressWarnings("WeakerAccess")
    public DateRange(Date bound) {
        this(bound, bound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    @SuppressWarnings("WeakerAccess")
    public DateRange(Date lowerBound, Date upperBound) {
        this(lowerBound, upperBound, BoundType.INCLUDING, BoundType.INCLUDING);
    }

    DateRange(Date lowerBound, Date upperBound, BoundType lowerBoundType, BoundType upperBoundType) {
        assert lowerBound != null;
        assert upperBound != null;
        this.lowerBoundType = lowerBoundType;
        this.upperBoundType = upperBoundType;
        Long localLowerBound = lowerBound.getTime();
        if (this.lowerBoundType == BoundType.EXCLUDING) {
            localLowerBound += 1;
        }
        Long localUpperBound = upperBound.getTime();
        if (this.upperBoundType == BoundType.EXCLUDING) {
            localUpperBound -= 1;
        }
        this.lowerBound = localLowerBound;
        this.upperBound = localUpperBound;
    }

    @SuppressWarnings("WeakerAccess")
    public DateRange(String source) {
        ParseStruct<Date> range = DateRangeParser.getInstance().parse(source);
        this.lowerBoundType = range.leftBoundType;
        this.upperBoundType = range.rightBoundType;

        Long lowerBound = range.min.getTime();
        if (this.lowerBoundType == BoundType.EXCLUDING) {
            lowerBound += 1;
        }
        Long upperBound = range.max.getTime();
        if (this.upperBoundType == BoundType.EXCLUDING) {
            upperBound -= 1;
        }
        if (lowerBound > upperBound) {
            throw new RuntimeException(range.max + " must be more or equal than " + range.min);
        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public Long getLowerBound() {
        return lowerBound;
    }

    public Long getUpperBound() {
        return upperBound;
    }

    public BoundType getLowerBoundType() {
        return lowerBoundType;
    }

    public BoundType getUpperBoundType() {
        return upperBoundType;
    }

    public boolean contains(Date d) {
        if (d == null) {
            return false;
        }
        long x = d.getTime();
        return lowerBound <= x && x <= upperBound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DateRange dateRange = (DateRange) o;
        return lowerBound == dateRange.lowerBound &&
                upperBound == dateRange.upperBound &&
                lowerBoundType == dateRange.lowerBoundType &&
                upperBoundType == dateRange.upperBoundType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lowerBound, upperBound, lowerBoundType, upperBoundType);
    }

    @Override
    public String toString() {
        SimpleDateFormat formatter = DateRangeParser.getDateTimeFormatter();
        StringBuilder sb = new StringBuilder();
        if (Long.MIN_VALUE == lowerBound) {
            sb.append(upperBoundType == BoundType.INCLUDING ? "<= " : "< ");
            sb.append(formatter.format(getDateUpperBound()));
        } else if (Long.MAX_VALUE == upperBound) {
            sb.append(lowerBoundType == BoundType.INCLUDING ? ">= " : "> ");
            sb.append(formatter.format(getDateLowerBound()));
        } else {
            sb.append(lowerBoundType == BoundType.INCLUDING ? '[' : '(');
            sb.append(formatter.format(getDateLowerBound()));
            sb.append("; ");
            sb.append(formatter.format(getDateUpperBound()));
            sb.append(upperBoundType == BoundType.INCLUDING ? ']' : ')');
        }
        return sb.toString();
    }

    private Date getDateUpperBound() {
        long time = upperBound;
        if (this.upperBoundType == BoundType.EXCLUDING) {
            time += 1;
        }
        return new Date(time);
    }

    private Date getDateLowerBound() {
        long time = lowerBound;
        if (this.lowerBoundType == BoundType.EXCLUDING) {
            time -= 1;
        }
        return new Date(time);
    }

    //CAST METHODS
    public static DateRange cast(Calendar x, StringRange y) {
        return new DateRange(x.getTime());
    }

    public static DateRange cast(long x, StringRange y) {
        return new DateRange(new Date(x));
    }
    //END

}
