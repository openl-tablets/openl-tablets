package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;

import java.lang.reflect.Array;

class String2ClassConvertor implements IString2DataConvertor<Class<?>> {

    @Override
    public String format(Class<?> data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public Class<?> parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

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

        if (data.endsWith("[]")) {
            Class<?> elementType = c.getInstanceClass();
            Object array = Array.newInstance(elementType, 0);
            return array.getClass();
        }

        return c.getInstanceClass();
    }
}
