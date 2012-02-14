package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;

public class String2OpenClassConvertor implements IString2DataConvertor {

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {

        if (data.endsWith("[]")) {

            String baseCode = data.substring(0, data.length() - 2);
            IOpenClass baseType = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, baseCode);

            if (baseType == null) {
                return null;
            }

            return baseType.getAggregateInfo().getIndexedAggregateType(baseType, 1);
        }

        IOpenClass c = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, data);

        if (c == null) {
            throw new RuntimeException("Type " + data + " is not found");
        }

        return c;
    }

}
