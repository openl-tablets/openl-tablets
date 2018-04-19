package org.openl.rules.convertor;

import org.openl.util.RuntimeExceptionWrapper;

import java.lang.reflect.Constructor;

class String2ConstructorConvertor<T> implements IString2DataConvertor<T> {

    private Constructor<T> ctr;

    public String2ConstructorConvertor(Class<T> clazz) {
        try {
            ctr = clazz.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException t) {
            throw new IllegalArgumentException("Public Constructor " + clazz.getName() + "(String s) does not exist");
        }
    }

    @Override
    public T parse(String data, String format) {
        if (data == null) return null;

        try {
            return ctr.newInstance(data);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}
