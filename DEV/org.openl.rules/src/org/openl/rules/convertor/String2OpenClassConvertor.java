package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;

class String2OpenClassConvertor implements IString2DataConvertor<IOpenClass> {

    @Override
    public String format(IOpenClass data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public IOpenClass parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

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
