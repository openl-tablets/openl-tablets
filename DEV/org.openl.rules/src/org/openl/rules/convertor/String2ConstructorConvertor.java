package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

import java.lang.reflect.Constructor;

class String2ConstructorConvertor implements IString2DataConvertor<Object> {

    private Constructor<?> ctr;

    public String2ConstructorConvertor(Constructor<?> ctr) {
        this.ctr = ctr;
    }

    @Override
    public String format(Object data, String format) {
        if (data == null) return null;
        return data.toString();
    }

    @Override
    public Object parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

        try {
            return ctr.newInstance(new Object[]{data});
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}
