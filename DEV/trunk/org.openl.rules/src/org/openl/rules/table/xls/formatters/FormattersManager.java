package org.openl.rules.table.xls.formatters;

import org.openl.util.formatters.DefaultFormatter;
import org.openl.util.formatters.FormatterAdapter;
import org.openl.util.formatters.IFormatter;
import org.openl.util.print.DefaultFormat;

/**
 * Manager to get the formatters for convertions from <code>Object</code> values to <code>String</code>.
 * If you need both side convertions use {@link XlsFormattersManager}.<br>
 * <b>NOTE!</b> This is a temporary class, to organize the formatters functionality in one place. 
 * 
 * @author DLiauchuk
 *
 */
public class FormattersManager {
    
    /**
     * Gets the formatter for appropriate value. Use this manager to get the appropriate formatter just for convertions 
     * from <code>Object</code> values to <code>String</code>.       
     * Formatters supports formatting of <code>null</code> objects, also see {@link XlsFormattersManager#getFormatter(Class, String)},
     * and {@link DefaultFormat}.
     * 
     * @param value
     * @return xls formatter for appropriate value class (if the value isn`t <code>null</code>). If value is <code>null</code>
     * or there is no xls formatter for it`s class, returns {@link FormatterAdapter}
     */
    public static IFormatter getFormatter(Object value) {
        IFormatter formatter = null;
        if (value != null) {
            Class<?> clazz = value.getClass();
            formatter = XlsFormattersManager.getFormatter(clazz);
            if (formatter instanceof DefaultFormatter) { // this is formatter used by default, we don`t like it, 
                                                            // so we try to format the value by other way.
                formatter = new FormatterAdapter();
            }
        } else {
            formatter = new FormatterAdapter();
        }
        
        return formatter;
    }
    
    public static IFormatter getFormatter(Class<?> clazz, String format) {
        return XlsFormattersManager.getFormatter(clazz, format);
    }

}
