package org.openl.rules.convertor;

import java.lang.reflect.Array;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;

public class String2ClassConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {

        String typeName;
        if (data.endsWith("[]")) {
            typeName = data.substring(0, data.length() - 2);
        } else {
            typeName = data;
        }
        IOpenClass c = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (c == null) {
            throw new RuntimeException("Type " + data + " is not found");
        }
        
        if(data.endsWith("[]")) {
            Class<?> elementType = c.getInstanceClass();
            Object array = Array.newInstance(elementType, 0);
            return array.getClass();
        }

        return c.getInstanceClass();
    }
}
