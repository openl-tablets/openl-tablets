package org.openl.rules.convertor;

import java.lang.reflect.Constructor;

import org.openl.binding.IBindingContext;
import org.openl.util.RuntimeExceptionWrapper;

public class String2ConstructorConvertor implements IString2DataConvertor {

    private Constructor<?> ctr;

    public String2ConstructorConvertor(Constructor<?> ctr) {
        this.ctr = ctr;
    }

    public String format(Object data, String format) {
        return String.valueOf(data);
    }

    public Object parse(String data, String format, IBindingContext cxt) {

        try {
            return ctr.newInstance(new Object[] { data });
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

}
