package org.openl.rules.table.formatters;

import java.math.BigDecimal;
import java.util.Locale;

import org.openl.meta.BigDecimalValue;
import org.openl.rules.helpers.NumberUtils;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberFormatter;

public class SmartNumberFormatter implements IFormatter {

    private Locale locale;

    public SmartNumberFormatter() {
        locale = Locale.US;
    }

    public SmartNumberFormatter(Locale locale) {
        this.locale = locale;
    }

    @Override
    public String format(Object value) {
        if (!(value instanceof Number)) {
            return null;
        }

        String format = getFormatForScale((Number) value);
        Number processedValue = getProcessedValue((Number) value);
        NumberFormatter formatter = new NumberFormatter(format, locale);
        return formatter.format(processedValue);
    }

    @Override
    public Object parse(String value) {
        throw new UnsupportedOperationException();
    }

    private static Number getProcessedValue(Number value) {
        if (value == null) {
            throw new NullPointerException("Null value is not supported");
        }

        if (value instanceof BigDecimal) {
            return value;
        }

        if (value instanceof BigDecimalValue) {
            return value;
        }

        if (NumberUtils.isObjectFloatPointNumber(value)) {
            /**
             * Process as float point value
             */
            double d = NumberUtils.convertToDouble(value);
            double d1 = d;
            double d2 = d;
            int scale = NumberUtils.getScale(d);
            int bestScale = scale;
            double best = d;
            for (int i = 0; i < 2; i++) {
                d1 = d1 - Math.ulp(d1);
                d2 = d2 + Math.ulp(d2);
                int s = NumberUtils.getScale(d1);
                if (s < bestScale) {
                    bestScale = s;
                    best = d1;
                }
                s = NumberUtils.getScale(d2);
                if (s < bestScale) {
                    bestScale = s;
                    best = d2;
                }
            }
            if (scale - bestScale > 1) {
                return best;
            } else {
                return d;
            }
        } else {
            return value;
        }
    }

    private static int getScale(Number value) {
        if (value == null) {
            throw new NullPointerException("Null value is not supported");
        }

        if (value instanceof BigDecimal) {
            /**
             * If BigDecimal the scale can be taken directly
             */
            return ((BigDecimal) value).scale();
        }

        if (value instanceof BigDecimalValue) {
            /**
             * If BigDecimalValue the scale can be taken directly
             */
            return ((BigDecimalValue) value).getValue().scale();
        }

        if (NumberUtils.isObjectFloatPointNumber(value)) {
            /**
             * Process as float point value
             */
            double d = NumberUtils.convertToDouble(value);
            double d1 = d;
            double d2 = d;
            int scale = NumberUtils.getScale(d);
            int bestScale = scale;
            for (int i = 0; i < 2; i++) {
                d1 = d1 - Math.ulp(d1);
                d2 = d2 + Math.ulp(d2);
                int s = NumberUtils.getScale(d1);
                if (s < bestScale) {
                    bestScale = s;
                }
                s = NumberUtils.getScale(d2);
                if (s < bestScale) {
                    bestScale = s;
                }
            }
            if (scale - bestScale > 1) {
                return bestScale;
            } else {
                return scale;
            }
        } else {
            /**
             * Process as integer value
             */
            return BigDecimal.valueOf(value.longValue()).scale();
        }
    }

    private static String getFormatForScale(Number value) {
        if (value != null) {
            int scale = getScale(value);
            StringBuilder buf = new StringBuilder();
            buf.append("#");
            if (scale > 0) {
                buf.append(".");

                for (int i = 0; i < scale; i++) {
                    buf.append("#");
                }
            }
            return buf.toString();
        }
        return null;
    }
}
