package org.openl.rules.table.xls.formatters;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.table.FormattedCell;
import org.openl.util.EnumUtils;

public class XlsEnumFormatter extends AXlsFormatter {

    private static final Log LOG = LogFactory.getLog(XlsEnumFormatter.class);

    private Class<?> enumClass;
    
    public XlsEnumFormatter(Class<?> enumType) {
        this.enumClass = enumType;
    }
    
    public String format(Object value) {

        if (!(value instanceof Enum<?>)) {
            
            LOG.error(String.format("Should be a %s value: %s" , enumClass.toString(),
                    ObjectUtils.toString(value, null)));
            return null;
        }
        
        return EnumUtils.getName((Enum<?>)value);        
    }
    
    public Object parse(String value) {
        return EnumUtils.valueOf(enumClass, value);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        return cell;
    }
}
