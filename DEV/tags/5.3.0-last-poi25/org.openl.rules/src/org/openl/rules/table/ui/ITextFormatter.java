/**
 * Created Feb 28, 2007
 */
package org.openl.rules.table.ui;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.openl.util.Log;

/**
 * @author snshor
 *
 */
public interface ITextFormatter {
    static public class ConstTextFormatter implements ITextFormatter {
        String format;

        public ConstTextFormatter(String format) {
            this.format = format;
        }

        public String format(Object obj) {
            return format;
        }

        public Object parse(String value) {
            return value;
        }
    }

    static public class DateTextFormatter implements ITextFormatter {
        SimpleDateFormat format;

        public DateTextFormatter(String fmt) {
            format = new SimpleDateFormat(fmt);
        }

        public String format(Object obj) {
            return format.format(obj);
        }

        public Object parse(String value) {
            try {
                return format.parse(value);
            } catch (ParseException e) {
                Log.warn("Could not parse date: " + value);
                return value;
            }
        }
    }

    static public class NumberTextFormatter implements ITextFormatter {
        DecimalFormat format;
        String fmtStr;

        public NumberTextFormatter(DecimalFormat fmt, String fmtStr) {
            format = fmt;
            this.fmtStr = fmtStr;
        }

        public NumberTextFormatter(String fmt) {
            format = new DecimalFormat(fmt);
        }

        public String format(Object obj) {
            return format.format(obj);
        }

        public Object parse(String value) {
            try {
                return format.parse(value);
            } catch (ParseException e) {
                Log.warn("Could not parse number: " + value);
            }

            try {
                return DateFormat.getDateInstance().parse(value);
            } catch (ParseException pe) {
                return value;
            }

        }
    }

    public String format(Object obj);

    public Object parse(String value);
}
