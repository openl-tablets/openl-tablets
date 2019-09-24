package org.openl.rules.convertor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.openl.util.RuntimeExceptionWrapper;

class String2ConstructorConvertor<T> implements IString2DataConvertor<T> {

    private Constructor<T> ctr;
    private Method m;

    public String2ConstructorConvertor(Class<T> clazz) {
        try {
            ctr = clazz.getDeclaredConstructor(String.class);
        } catch (NoSuchMethodException t) {
            try {
                m = clazz.getDeclaredMethod("valueOf", String.class);
            } catch (NoSuchMethodException e) {
                try {
                    m = clazz.getDeclaredMethod("parse", CharSequence.class);
                } catch (NoSuchMethodException ex) {
                    throw new IllegalArgumentException("Neither public Constructor " + clazz
                        .getName() + "(String s), nor public static valueOf(String s), nor public static parse(CharSequence s) do not exist",
                        ex);
                }
            }
        }
    }

    @Override
    public T parse(String data, String format) {
        if (data == null) {
            return null;
        }

        try {
            return ctr != null ? ctr.newInstance(data) : (T) m.invoke(null, data);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}
