package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.types.IOpenClass;

class String2OpenClassConvertor implements IString2DataConvertor<IOpenClass>, IString2DataConverterWithContext<IOpenClass> {

    public static final String ARRAY_SUFFIX = "[]";

    @Override
    public IOpenClass parse(String data, String format) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IOpenClass parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

        String typeName;
        if (data.endsWith(ARRAY_SUFFIX)) {
            typeName = data.substring(0, data.length() - 2);
        } else {
            typeName = data;
        }

        IOpenClass openClass = cxt.findType(ISyntaxConstants.THIS_NAMESPACE, typeName);

        if (openClass == null) {
            throw new IllegalArgumentException("Type " + data + " is not found");
        }

        if (data.endsWith(ARRAY_SUFFIX)) {
            openClass = openClass.getAggregateInfo().getIndexedAggregateType(openClass);
        }
        return openClass;
    }
}
