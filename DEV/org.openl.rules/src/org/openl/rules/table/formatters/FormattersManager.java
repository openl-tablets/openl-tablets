package org.openl.rules.table.formatters;

import org.openl.rules.helpers.NumberUtils;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.openl.util.formatters.*;

import java.util.Date;
import java.util.Locale;

/**
 * Manager to get the formatters for convertions from <code>Object</code> values to <code>String</code> and vice versa.
 *
 * @author DLiauchuk
 */
public class FormattersManager {

    public static String DEFAULT_DATE_FORMAT = "MM/dd/yyyy";
    public static String DEFAULT_NUMBER_FORMAT = "#.#################";

    /**
     * Gets the formatter for appropriate value. Formatters supports formatting of <code>null</code> objects.
     *
     * @param value value to format
     * @return xls formatter for appropriate value class (if the value isn`t <code>null</code>). If value is
     *         <code>null</code> or there is no formatter for it`s class, returns {@link FormatterAdapter}
     */
    static IFormatter getFormatter(Object value) {
        IFormatter formatter;
        if (value != null) {
            Class<?> clazz = value.getClass();
            formatter = getFormatter(clazz, value);
            if (formatter instanceof DefaultFormatter) { // this is formatter
                // used by default, we
                // don`t like it,
                // so we try to format
                // the value by other
                // way.
                formatter = new FormatterAdapter();
            }
        } else {
            formatter = new FormatterAdapter();
        }

        return formatter;
    }

    /**
     * Returns String presentation of the given object. This method do a little bit more than Java's toString(). It
     * returns a human readable string for Enums, Arrays, Booleans, Dates...
     *
     * @param value an object to format
     * @return String presentation of the object
     * @see #getFormatter(Object)
     */
    public static String format(Object value) {
        return getFormatter(value).format(value);
    }

    /**
     * The method used for getting the appropriate formatter for the income class. If no formatter found it will be
     * returned {@link DefaultFormatter} as default.<br>
     * Existing formatters:
     * <ul>
     * <li>{@link NumberFormatter} for numeric types.</li>
     * <li>{@link DateFormatter} for {@link Date} type.</li>
     * <li>{@link BooleanFormatter} for {@link Boolean} type.</li>
     * <li>{@link EnumFormatter} for Enum types.</li>
     * <li>{@link ArrayFormatter} for array types, also supports primitive arrays.</li>
     * <li>{@link DefaultFormatter} for {@link String} type.</li>
     * </ul>
     *
     * @param clazz formatter will be returned for this {@link Class}.
     * @param format format for number, date formatters. If <code>null</code> default format will be used
     *            {@link #DEFAULT_NUMBER_FORMAT} and {@link #DEFAULT_DATE_FORMAT} accordingly.
     * @return formatter for a type.
     */
    public static IFormatter getFormatter(Class<?> clazz, Object value, String format) {
        IFormatter formatter;

        // Numeric
        if (ClassUtils.isAssignable(clazz, Number.class)) {
            if (StringUtils.isBlank(format) && NumberUtils.isFloatPointType(clazz)) {
                format = getFormatForScale(value);
                return new SmartNumberFormatter(Locale.US);
            }
            String numberFormat = StringUtils.isNotBlank(format) ? format : DEFAULT_NUMBER_FORMAT;
            formatter = new NumberFormatter(numberFormat, Locale.US);
            // Date
        } else if (clazz == Date.class) {
            String dateFormat = StringUtils.isNotBlank(format) ? format : DEFAULT_DATE_FORMAT;
            formatter = new DateFormatter(dateFormat);

            // Boolean
        } else if (ClassUtils.isAssignable(clazz, Boolean.class)) {
            formatter = new BooleanFormatter();

            // Enum
        } else if (clazz.isEnum()) {
            formatter = new EnumFormatter(clazz);

            // Array
        } else if (clazz.isArray()) {
            Class<?> componentType = clazz.getComponentType();
            IFormatter componentFormatter = getFormatter(componentType);
            formatter = new ArrayFormatter(componentFormatter);

            // If formatter wasn`t found use DefaultFormat as default.
        } else {
            formatter = new DefaultFormatter();
        }

        return formatter;
    }

    /**
     * Returns format pattern for appropriate value scale
     *
     * @param value float point value which is gonna be formatted
     * @return format pattern for appropriate value scale, <code>null</code> if value is <code>null</code>
     */
    private static String getFormatForScale(Object value) {
        if (value != null) {
            int scale = NumberUtils.getScale((Number) value);

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

    public static IFormatter getFormatter(Class<?> clazz, Object value) {
        return getFormatter(clazz, value, null);
    }

    public static IFormatter getFormatter(Class<?> clazz, String format) {
        return getFormatter(clazz, null, format);
    }

    public static IFormatter getFormatter(Class<?> clazz) {
        return getFormatter(clazz, null, null);
    }

}
