package org.openl.rules.convertor;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalQueries;
import java.util.Calendar;
import java.util.Date;

class String2DateConvertor implements IString2DataConvertor<Date> {

    /**
     * By default, Excel determines the century by using a cutoff year of 2029. See:
     * https://learn.microsoft.com/en-us/office/troubleshoot/excel/two-digit-year-numbers#the-2029-rule
     */
    private static final LocalDate BASE_DATE = LocalDate.of(1930, 1, 1);

    @Override
    public Date parse(String data, String format) {
        if (data == null) {
            return null;
        }
        if (format != null) {
            DateFormat df = new SimpleDateFormat(format, LocaleDependConvertor.getLocale());
            try {
                return df.parse(data);
            } catch (ParseException e) {
                throw new IllegalArgumentException(
                        String.format("Cannot convert '%s' to date type using: '%s' format", data, format));
            }
        }

        try {
            // Special case for the two digit year in US format.
            LocalDateTime localDateTime = new DateTimeFormatterBuilder().appendPattern("M/d/")
                    .appendValueReduced(ChronoField.YEAR, 2, 2, BASE_DATE)
                    .parseStrict()
                    .toFormatter(LocaleDependConvertor.getLocale())
                    .parse(data, TemporalQueries.localDate())
                    .atTime(0, 0);
            return Date.from(localDateTime.toInstant(ZoneId.systemDefault().getRules().getOffset(localDateTime)));
        } catch (Exception ignore) {
            // Ignore
        }

        DateTimeFormatterBuilder patternBuilder = new DateTimeFormatterBuilder()
                .appendPattern("y-M-d['T'H:m[:s[.")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, false)
                .appendPattern("]][X]]");
        Date cal = parsePattern(data, patternBuilder);
        if (cal != null) return cal;

        patternBuilder = new DateTimeFormatterBuilder()
                .appendPattern("y-M-d['T'H:m[:s[.")
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, false)
                .appendPattern("]][XXX]]");
        cal = parsePattern(data, patternBuilder);
        if (cal != null) return cal;

        patternBuilder = new DateTimeFormatterBuilder().appendPattern("M/d/y[ H:m[:s]]");
        cal = parsePattern(data, patternBuilder);
        if (cal != null) return cal;

        patternBuilder = new DateTimeFormatterBuilder().appendPattern("M/d/y hh:mm a");
        cal = parsePattern(data, patternBuilder);
        if (cal != null) return cal;

        throw new IllegalArgumentException(String.format("Cannot convert '%s' to Date type", data));
    }

    private static Date parsePattern(String data, DateTimeFormatterBuilder patternBuilder) {
        var parser = patternBuilder
                .parseStrict()
                .toFormatter(LocaleDependConvertor.getLocale());
        var parsePosition = new ParsePosition(0);
        try {
            var result = parser.parse(data, parsePosition);
            if (parsePosition.getIndex() != data.length()) {
                return null;
            }
            var date = result.query(TemporalQueries.localDate());
            var time = result.query(TemporalQueries.localTime());

            var cal = Calendar.getInstance();
            cal.clear();
            cal.set(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());

            if (time != null) {
                cal.set(Calendar.HOUR_OF_DAY, time.getHour());
                cal.set(Calendar.MINUTE, time.getMinute());
                cal.set(Calendar.SECOND, time.getSecond());
                cal.set(Calendar.MILLISECOND, time.getNano() / 1_000_000);
            }

            if (result.isSupported(ChronoField.OFFSET_SECONDS)) {
                cal.set(Calendar.ZONE_OFFSET, result.get(ChronoField.OFFSET_SECONDS) * 1_000);
                cal.set(Calendar.DST_OFFSET, 0);
            }

            return cal.getTime();
        } catch (Exception ignore) {
            return null;
        }
    }

}
