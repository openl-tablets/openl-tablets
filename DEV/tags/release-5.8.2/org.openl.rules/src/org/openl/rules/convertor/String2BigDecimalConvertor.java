package org.openl.rules.convertor;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.openl.binding.IBindingContext;

public class String2BigDecimalConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        if (data != null) {
            return String.valueOf(data);
        }
        return null;
        
    }

    public Object parse(String data, String format, IBindingContext bindingContext) {     
        if (StringUtils.isNotBlank(data)) {
            return new BigDecimal(data);
        }
        return null;
    }

}
