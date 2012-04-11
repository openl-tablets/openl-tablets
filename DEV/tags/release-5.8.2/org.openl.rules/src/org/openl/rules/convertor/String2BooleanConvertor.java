package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.util.BooleanUtils;

public class String2BooleanConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        if (data == null || data.length() == 0) {
            return Boolean.FALSE;
        }            
        
        Boolean boolValue = BooleanUtils.toBooleanObject(data);
        
        if (boolValue != null) {
            return boolValue;
        } else {
            throw new RuntimeException("Invalid boolean value: " + data);
        }            
    }

}
