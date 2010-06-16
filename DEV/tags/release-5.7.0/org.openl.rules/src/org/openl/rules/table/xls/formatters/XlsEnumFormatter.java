package org.openl.rules.table.xls.formatters;

import org.apache.commons.lang.ObjectUtils;
import org.openl.rules.table.FormattedCell;
import org.openl.util.EnumUtils;
import org.openl.util.Log;

public class XlsEnumFormatter extends AXlsFormatter {

    private Class<?> enumClass;
    
    public XlsEnumFormatter(Class<?> enumType) {
        this.enumClass = enumType;
    }
    
    public String format(Object value) {

        if (!(value instanceof Enum<?>)) {
            
            Log.error(String.format("Should be a %s value: %s" , enumClass.toString(),
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
