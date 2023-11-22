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
import java.util.List;

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

        for (var pattern : List.of("y-M-d['T'H:m[:s[.SSS]][X]]", "M/d/y[ H:m[:s]]", "M/d/y hh:mm a")) {
            try {
                var parsePosition = new ParsePosition(0);
                var parser = new DateTimeFormatterBuilder().appendPattern(pattern)
                    .parseStrict()
                    .toFormatter(LocaleDependConvertor.getLocale());
                var result = parser.parse(data, parsePosition);
                if (parsePosition.getIndex() == data.length()) {
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
                }
            } catch (Exception ignore) {
                // Loop on
            }
        }
        throw new IllegalArgumentException(String.format("Cannot convert '%s' to Date type", data));
    }

}
