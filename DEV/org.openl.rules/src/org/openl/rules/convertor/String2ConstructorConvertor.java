package org.openl.rules.convertor;

import java.util.function.Function;

import org.openl.util.RuntimeExceptionWrapper;

class String2ConstructorConvertor<T> implements IString2DataConvertor<T> {

    private final Function<String, T> parser;

    public String2ConstructorConvertor(Class<T> clazz) {
        this.parser = findParser(clazz);
    }

    /**
     * Finds how the class reads itself from a text: a String constructor, then a static valueOf(String), then a
     * static parse(CharSequence).
     */
    @SuppressWarnings("unchecked")
    private static <T> Function<String, T> findParser(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor(String.class);
            return data -> invoke(() -> constructor.newInstance(data));
        } catch (NoSuchMethodException t) {
            try {
                var valueOf = clazz.getDeclaredMethod("valueOf", String.class);
                return data -> invoke(() -> (T) valueOf.invoke(null, data));
            } catch (NoSuchMethodException e) {
                try {
                    var parse = clazz.getDeclaredMethod("parse", CharSequence.class);
                    return data -> invoke(() -> (T) parse.invoke(null, data));
                } catch (NoSuchMethodException ex) {
                    throw new IllegalArgumentException("Neither public constructor '%s(String s)', nor public static method 'valueOf(String s)', nor public static method 'parse(CharSequence s)' is not found.".formatted(
                            clazz.getTypeName()), ex);
                }
            }
        }
    }

    private static <T> T invoke(ReflectiveCall<T> call) {
        try {
            return call.get();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @FunctionalInterface
    private interface ReflectiveCall<T> {
        T get() throws ReflectiveOperationException;
    }

    @Override
    public T parse(String data, String format) {
        if (data == null) {
            return null;
        }
        return parser.apply(data);
    }
}
