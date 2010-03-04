package org.openl.rules.table.xls;

import org.openl.rules.table.FormattedCell;
import org.openl.util.EnumUtils;
import org.openl.util.Log;

public class XlsEnumFormat extends XlsFormat {

    private Class<?> enumClass;
    
    public XlsEnumFormat(Class<?> enumType) {
        this.enumClass = enumType;
    }

    @Override
    public String format(Object value) {

        if (!(value instanceof Enum)) {
            
            Log.error(String.format("Should be a %s value: %s" , enumClass.toString(), value.toString()));
            return null;
        }
        
        return EnumUtils.getName((Enum<?>)value);        
    }

    @Override
    public Object parse(String value) {
        return EnumUtils.valueOf(enumClass, value);
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        return cell;
    }
}
