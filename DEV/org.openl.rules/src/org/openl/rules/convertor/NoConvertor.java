package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

class NoConvertor implements IString2DataConvertor<Object> {

    private final String className;

    public NoConvertor(Class<?> clazz) {
        this.className = clazz.getName();
    }

    @Override
    public String format(Object data, String format) {
        throw new RuntimeException("Should not call this method");
    }

    @Override
    public Object parse(String data, String format, IBindingContext cxt) {
        // FIXME: Wrong exception type. The error about not existing
        // converter must be thrown in corresponding factory. Throwing error
        // from looks like ugly design.
        throw new IllegalArgumentException("Convertor or Public Constructor " + className + "(String s) does not exist");
    }
}
