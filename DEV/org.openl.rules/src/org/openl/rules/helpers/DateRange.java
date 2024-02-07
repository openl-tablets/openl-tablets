package org.openl.rules.helpers;

import java.beans.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.helpers.ARangeParser.ParseStruct.BoundType;
import org.openl.rules.range.Range;

public class DateRange extends Range<Date> {

    private static final int TO_DATE_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;
    private static final DateTimeFormatter dateTimeParser = DateTimeFormatter.ofPattern("M/d/yyyy[ H:m:s]");
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy[ HH:mm:ss]");

    private final long lowerBound;
    private final long upperBound;

    private final Type type;

    public DateRange(Date bound) {
        this.lowerBound = bound.getTime();
        this.upperBound = bound.getTime();
        this.type = Type.DEGENERATE;
    }

    public DateRange(Date lowerBound, Date upperBound) {
        this.lowerBound = lowerBound.getTime();
        this.upperBound = upperBound.getTime();
        this.type = Type.CLOSED;
        validate();
    }

    DateRange(Date lowerBound, Date upperBound, BoundType lowerBoundType, BoundType upperBoundType) {

        assert lowerBound != null;
        assert upperBound != null;
        long localLowerBound = lowerBound.getTime();
        long localUpperBound = upperBound.getTime();

        if (Long.MAX_VALUE == localUpperBound) {
            this.type = lowerBoundType == BoundType.EXCLUDING ? Type.LEFT_OPEN : Type.LEFT_CLOSED;
        } else if (Long.MIN_VALUE == localLowerBound) {
            this.type = upperBoundType == BoundType.EXCLUDING ? Type.RIGHT_OPEN : Type.RIGHT_CLOSED;
        } else if (upperBoundType == BoundType.EXCLUDING) {
            this.type = lowerBoundType == BoundType.EXCLUDING ? Type.OPEN : Type.CLOSED_OPEN;
        } else {
            this.type = lowerBoundType == BoundType.EXCLUDING ? Type.OPEN_CLOSED : Type.CLOSED;
        }

        this.lowerBound = localLowerBound;
        this.upperBound = localUpperBound;
        validate();
    }

    public DateRange(String source) {
        var parser = parse(source);
        if (parser == null) {
            this.type = Type.DEGENERATE;
            this.lowerBound = convertToTime(source.trim());
            this.upperBound = this.lowerBound;
        } else {
            this.type = parser.getType();
            var left = parser.getLeft();
            var right = parser.getRight();
            this.lowerBound = left == null ? Long.MIN_VALUE : convertToTime(left);
            this.upperBound = right == null ? Long.MAX_VALUE : convertToTime(right);
            validate();
        }
    }

    public Long getLowerBound() {
        return lowerBound;
    }

    public Long getUpperBound() {
        return upperBound;
    }

    @Override
    public boolean contains(Date value) {
        return super.contains(value);
    }

    @Override
    @Transient
    public Type getType() {
        return type;
    }

    @Override
    protected Date getLeft() {
        return new Date(lowerBound);
    }

    @Override
    protected Date getRight() {
        return new Date(upperBound);
    }

    @Override
    protected int compare(Date left, Date right) {
        return Long.compare(left.getTime(), right.getTime());
    }

    @Override
    protected void format(StringBuilder sb, Date value) {
        LocalDateTime time = Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        sb.append(dateTimeFormatter.format(time));
    }

    // AUTOCAST METHODS
    public static DateRange autocast(Date x, DateRange y) {
        return new DateRange(x);
    }

    public static int distance(Date x, DateRange y) {
        return TO_DATE_RANGE_CAST_DISTANCE;
    }

    public static DateRange autocast(Calendar x, DateRange y) {
        return new DateRange(x.getTime());
    }

    public static int distance(Calendar x, DateRange y) {
        return TO_DATE_RANGE_CAST_DISTANCE;
    }

    public static DateRange autocast(long x, DateRange y) {
        return new DateRange(new Date(x));
    }

    public static int distance(long x, DateRange y) {
        return TO_DATE_RANGE_CAST_DISTANCE;
    }

    // END

    private static long convertToTime(String text) {
        TemporalAccessor res = dateTimeParser.parseBest(text, LocalDateTime::from, LocalDate::from);
        LocalDateTime localDateTime = res instanceof LocalDate ? ((LocalDate) res).atStartOfDay() : (LocalDateTime) res;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
