package org.openl.rules.table.xls;
import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.openl.rules.table.ui.AGridFilter;
import org.openl.rules.table.ui.IGridFilter;

public abstract class XlsFormat extends AGridFilter {
    
    private static HashMap<Class<?>, XlsFormat> formatters;
    
    static {        
        formatters = new HashMap<Class<?>, XlsFormat>();

        formatters.put(Integer.class, XlsNumberFormat.General);
        formatters.put(Double.class, XlsNumberFormat.General);
        formatters.put(Boolean.class, new XlsBooleanFormat());
        formatters.put(Date.class, new XlsDateFormat(XlsDateFormat.DEFAULT_XLS_DATE_FORMAT));
        formatters.put(String.class, new XlsStringFormat());
    }
    
    public abstract String format(Object value);
    
    /**
     * The method used for getting the appropriate formatter for the income class. If no formatter found
     * it will be returned {@link XlsStringFormat} as default.<br>
     * Existing formatters:<ul>
     *      <li>{@link XlsNumberFormat} for {@link Integer} and {@link Double} types.</li>
     *      <li>{@link XlsBooleanFormat} for {@link Boolean} type.</li>
     *      <li>{@link XlsDateFormat} for {@link Date} type.</li>
     *      <li>{@link XlsStringFormat} for {@link String} type.</li>
     *      <li>{@link XlsEnumFormat} for Enum types.</li>
     *      <li>{@link XlsArrayFormat} for array types.</li>
     * </ul>
     * 
     * @param clazz formatter will be returned for this {@link Class}.
     * @param format custom format for date formatter. If <code>null</code> default format will be used 
     * {@link XlsDateFormat#DEFAULT_XLS_DATE_FORMAT}.
     * @return formatter for a type.
     */
    public static XlsFormat getFormatter(Class<?> clazz, String format) {
        XlsFormat formatter = formatters.get(clazz);
        
        if (formatter != null) {
            // apply users format for date formatter instead of default one from initialization.
            if (formatter instanceof XlsDateFormat && StringUtils.isNotEmpty(format)) {
                ((XlsDateFormat)formatter).setFormat(format);
            }
        } else {            
            if (clazz.isEnum()) {
                formatter = new XlsEnumFormat(clazz);
            } else  if (clazz.isArray()) {
                Class<?> componentType = clazz.getComponentType();
                IGridFilter componentFilter = getFormatter(componentType, null);
                formatter = new XlsArrayFormat((XlsFormat) componentFilter);
            }
        }
        
        // if formatter wasn`t found use XlsStringFormat as default.
        if (formatter == null) {            
            formatter = new XlsStringFormat();
        }
        return formatter;
    }
}
