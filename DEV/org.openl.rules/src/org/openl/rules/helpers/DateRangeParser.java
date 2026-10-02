package org.openl.rules.helpers;

import java.text.ParseException;
import java.util.regex.Pattern;

import org.openl.rules.range.RangeParser;

public class DateRangeParser {

    /**
     * A date in the US form {@code 12/31/2024} or in the ISO form {@code 2024-12-31}, with an optional time
     * {@code 23:59:59}. The time follows a space, or the letter {@code T} in the ISO form.
     */
    static final Pattern DATE = Pattern.compile("\\d{1,2}/\\d{1,2}/\\d+(?: \\d{1,2}:\\d{1,2}:\\d{1,2})?"
            + "|\\d{4}-\\d{1,2}-\\d{1,2}(?:[ T]\\d{1,2}:\\d{1,2}:\\d{1,2})?");

    private static final class InstanceHolder {
        private static final DateRangeParser INSTANCE = new DateRangeParser();
    }

    private DateRangeParser() {
    }

    public static DateRangeParser getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public boolean likelyRangeThanDate(String value) {
        try {
            var rangeParser = RangeParser.parse(value, DATE);
            if (rangeParser == null) {
                return false;
            }
            var left = rangeParser.getLeft();
            if (left != null && !DATE.matcher(left).matches()) {
                return false;
            }
            var right = rangeParser.getRight();
            return right == null || DATE.matcher(right).matches();
        } catch (ParseException e) {
            return false;
        }
    }

}
