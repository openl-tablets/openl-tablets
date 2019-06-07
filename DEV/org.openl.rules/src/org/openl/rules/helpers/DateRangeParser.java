package org.openl.rules.helpers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateRangeParser extends ARangeParser<Long> {

    /**
     * Parses strings like:
     *
     * 1/1/2019 01/01/2019 01/01/2019 2:2:2 1/1/2019 2:2:2 1/1/2019 02:02:02 01/01/2019 02:02:02
     */
    private static final DateTimeFormatter dateTimeParser = DateTimeFormatter.ofPattern("M/d/yyyy[ H:m:s]");

    // for date formatting which includes leading zeros in month, day, hour, minutes and seconds
    static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy[ HH:mm:ss]");

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

    public boolean likelyRangeThanDate(String value) {
        for (int i = 0; i < 5; i++) {
            Matcher m = patterns[i].matcher(value);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    RangeParser[] getRangeParsers() {
        return parsers;
    }

    private static final class DateRangeBoundAdapter implements RangeBoundAdapter<Long> {

        private static final Long MIN = Long.MIN_VALUE;
        private static final Long MAX = Long.MAX_VALUE;

        @Override
        public Long adaptValue(String s) {
            TemporalAccessor res = dateTimeParser.parseBest(s, LocalDateTime::from, LocalDate::from);
            return toInstant(res);
        }

        private Long toInstant(TemporalAccessor t) {
            LocalDateTime localDateTime = t instanceof LocalDate ? ((LocalDate) t).atStartOfDay() : ((LocalDateTime) t);
            return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        @Override
        public Long getMinLeftBound() {
            return MIN;
        }

        @Override
        public Long getMaxRightBound() {
            return MAX;
        }
    }

}
