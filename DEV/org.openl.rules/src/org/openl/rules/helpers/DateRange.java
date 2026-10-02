package org.openl.rules.helpers;

import java.beans.Transient;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import lombok.Getter;

import org.openl.binding.impl.cast.CastFactory;
import org.openl.rules.range.Range;

// A parameter that is not read selects the overload by its type: the engine resolves casts and distances by signature.
// Range.equals already compares the bounds this range keeps, reading them through getType, getLeft and getRight.
@SuppressWarnings({"java:S1172", "java:S2160"})
public class DateRange extends Range<Date> {

    private static final int TO_DATE_RANGE_CAST_DISTANCE = CastFactory.AFTER_FIRST_WAVE_CASTS_DISTANCE + 8;
    private static final DateTimeFormatter US_PARSER = DateTimeFormatter.ofPattern("M/d/yyyy[ H:m:s]", Locale.ROOT);
    private static final DateTimeFormatter ISO_PARSER = DateTimeFormatter.ofPattern("yyyy-M-d[ H:m:s]['T'H:m:s]",
            Locale.ROOT);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy[ HH:mm:ss]",
            Locale.ROOT);

    private final long lowerBound;
    private final long upperBound;

    @Getter(onMethod_ = {@Transient})
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

    /**
     * Creates a range from a text in one of the range forms, such as {@code [2024-01-01; 2024-07-01)},
     * {@code 2024-01-01 .. 2024-12-31} or {@code >= 12/01/2024}, or from a single date.
     *
     * <p>A date is written in the ISO form {@code 2024-12-31} or in the US form {@code 12/31/2024}, with an optional
     * time {@code 23:59:59}. The time follows a space, or the letter {@code T} in the ISO form. A date without a time
     * is the start of the day.
     *
     * <p>A date is a local date and time of the default time zone, like the other dates of the rules. The language
     * of the server does not change how a date is read.
     */
    public DateRange(String source) {
        var parser = parse(source, DateRangeParser.DATE);
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
        var time = Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        sb.append(FORMATTER.format(time));
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
        var parser = text.indexOf('/') < 0 ? ISO_PARSER : US_PARSER;
        var res = parser.parseBest(text, LocalDateTime::from, LocalDate::from);
        LocalDateTime localDateTime = res instanceof LocalDate ld ? ld.atStartOfDay() : (LocalDateTime) res;
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
