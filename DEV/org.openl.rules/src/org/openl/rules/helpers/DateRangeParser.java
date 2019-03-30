package org.openl.rules.helpers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateRangeParser extends ARangeParser<Date> {

    private static final String DEFAULT_DATE_PATTERN = "MM/dd/yyyy";
    private static final String DEFAULT_DATE_TIME_PATTERN = "MM/dd/yyyy HH:mm:ss";

    private static final class InstanceHolder {
        private static final DateRangeParser INSTANCE = new DateRangeParser();
    }

    private static final String BRACKETS_PATTERN = "\\s*([\\[(])\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*(?:[-;…]|\\.{2,3})\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*([])])\\s*";
    private static final String MIN_MAX_PATTERN = "\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*([-…]|\\.{2,3})\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*";
    private static final String VERBAL_PATTERN = "\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*(\\+|and more|or less)\\s*";
    private static final String MORE_LESS_PATTERN = "\\s*(<|>|>=|<=|less than|more than)\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*";
    private static final String RANGE_MORE_LESS_PATTERN = "\\s*(<=?|>=?)\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*(<=?|>=?)\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*";
    private static final String SIMPLE_PATTERN = "\\s*((?:\\d{1,2}/){2}\\d+(?: (?:\\d{1,2}:){2}\\d{1,2})?)\\s*";

    private final RangeParser[] parsers;
    private final Pattern[] patterns;

    private DateRangeParser() {
        patterns = new Pattern[] { Pattern.compile(BRACKETS_PATTERN),
                Pattern.compile(MIN_MAX_PATTERN),
                Pattern.compile(VERBAL_PATTERN),
                Pattern.compile(MORE_LESS_PATTERN),
                Pattern.compile(RANGE_MORE_LESS_PATTERN),
                Pattern.compile(SIMPLE_PATTERN) };
        DateRangeBoundAdapter adapter = new DateRangeBoundAdapter();
        parsers = new RangeParser[] { new BracketsParser<>(patterns[0], adapter),
                new MinMaxParser<>(patterns[1], adapter),
                new VerbalParser<>(patterns[2], adapter),
                new MoreLessParser<>(patterns[3], adapter),
                new RangeWithMoreLessParser<>(patterns[4], adapter),
                new SimpleParser<>(patterns[5], adapter) };
    }

    public static DateRangeParser getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public boolean isDateRange(String value) {
        for (Pattern pattern : patterns) {
            Matcher m = pattern.matcher(value);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

    public boolean canBeNotDateRange(String value) {
        Matcher m = patterns[5].matcher(value);
        return m.matches();
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class DateRangeBoundAdapter implements RangeBoundAdapter<Date> {

        @Override
        public Date adaptValue(String s) {
            try {
                return getDateTimeFormatter().parse(s);
            } catch (ParseException e1) {
                try {
                    return getDateFormatter().parse(s);
                } catch (ParseException e2) {
                    RuntimeException res = new IllegalArgumentException(e2);
                    res.addSuppressed(e1);
                    throw res;
                }
            }
        }

        @Override
        public Date getMinLeftBound() {
            return new Date(Long.MIN_VALUE);
        }

        @Override
        public Date getMaxRightBound() {
            return new Date(Long.MAX_VALUE);
        }
    }

    static SimpleDateFormat getDateFormatter() {
        return getDateFormatter(DEFAULT_DATE_PATTERN);
    }

    static SimpleDateFormat getDateTimeFormatter() {
        return getDateFormatter(DEFAULT_DATE_TIME_PATTERN);
    }

    private static SimpleDateFormat getDateFormatter(String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
        formatter.setLenient(false); // Strict matching
        formatter.getCalendar().set(0, 0, 0, 0, 0, 0); // at
        formatter.getCalendar().set(Calendar.MILLISECOND, 0);
        return formatter;
    }

}
