package org.openl.rules.table.formatters;

import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.util.formatters.BooleanFormatter;
import org.openl.util.formatters.DateFormatter;
import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.EnumFormatter;
import org.openl.util.formatters.FormatterAdapter;
import org.openl.util.formatters.IFormatter;
import org.openl.util.formatters.NumberFormatter;
import org.openl.util.print.DefaultFormat;

/**
 * Manager to get the formatters for convertions from <code>Object</code> values to <code>String</code> and vice versa.
 *  
 * @author DLiauchuk
 */
public class FormattersManager {

    public static String DEFAULT_DATE_FORMAT = "M/d/yy";
    public static String DEFAULT_NUMBER_FORMAT = "#.######";

    /**
     * Gets the formatter for appropriate value.       
     * Formatters supports formatting of <code>null</code> objects, see {@link DefaultFormat}.
     * 
     * @param value
     * @return xls formatter for appropriate value class (if the value isn`t <code>null</code>).
     * If value is <code>null</code> or there is no formatter for it`s class, returns {@link FormatterAdapter}
     */
    public static IFormatter getFormatter(Object value) {
        IFormatter formatter = null;
        if (value != null) {
            Class<?> clazz = value.getClass();
            formatter = getFormatter(clazz);
            if (formatter instanceof DefaultFormatter) { // this is formatter used by default, we don`t like it, 
                                                            // so we try to format the value by other way.
                formatter = new FormatterAdapter();
            }
        } else {
            formatter = new FormatterAdapter();
        }
        
        return formatter;
    }

    /**
     * The method used for getting the appropriate formatter for the income class. If no formatter found
     * it will be returned {@link DefaultFormatter} as default.<br>
     * Existing formatters:<ul>
     *      <li>{@link NumberFormatter} for numeric types.</li>
     *      <li>{@link DateFormatter} for {@link Date} type.</li>
     *      <li>{@link BooleanFormatter} for {@link Boolean} type.</li>
     *      <li>{@link EnumFormatter} for Enum types.</li>
     *      <li>{@link ArrayFormatter} for array types, also supports primitive arrays.</li>
     *      <li>{@link DefaultFormatter} for {@link String} type.</li>
     * </ul>
     * 
     * @param clazz formatter will be returned for this {@link Class}.
     * @param format format for number, date formatters. If <code>null</code> default format will be used 
     * {@link DEFAULT_NUMBER_FORMAT} and {@link DEFAULT_DATE_FORMAT} accordingly.
     * @return formatter for a type.
     */
    public static IFormatter getFormatter(Class<?> clazz, String format) {
        IFormatter formatter = null;

        // Numeric
        if (ClassUtils.isAssignable(clazz, Number.class, true)) {
            String numberFormat = StringUtils.isNotBlank(format) ? format : DEFAULT_NUMBER_FORMAT;
            formatter = new NumberFormatter(numberFormat, Locale.US);

        // Date
        } else if (clazz == Date.class) {
            String dateFormat = StringUtils.isNotBlank(format) ? format : DEFAULT_DATE_FORMAT;
            formatter = new DateFormatter(dateFormat);

        // Boolean
        } else if (ClassUtils.isAssignable(clazz, Boolean.class, true)) {
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

    public static IFormatter getFormatter(Class<?> clazz) {
        return getFormatter(clazz, null);
    }

}
