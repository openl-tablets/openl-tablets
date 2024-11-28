package org.openl.rules.helpers;

import java.text.ParseException;
import java.util.regex.Pattern;

import org.openl.rules.range.RangeParser;

public class DateRangeParser {

    // for date formatting which includes leading zeros in month, day, hour, minutes and seconds

    private static final class InstanceHolder {
        private static final DateRangeParser INSTANCE = new DateRangeParser();
    }

    private static final Pattern SIMPLE_PATTERN = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d+(?: \\d{1,2}:\\d{1,2}:\\d{1,2})?");


    private DateRangeParser() {
    }

    public static DateRangeParser getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public boolean likelyRangeThanDate(String value) {
        try {
            var rangeParser = RangeParser.parse(value);
            if (rangeParser == null) {
                return false;
            }
            var left = rangeParser.getLeft();
            if (left != null && !SIMPLE_PATTERN.matcher(left).matches()) {
                return false;
            }
            var right = rangeParser.getRight();
            if (right != null && !SIMPLE_PATTERN.matcher(right).matches()) {
                return false;
            }
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

}
