package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

class String2EnumConvertor<E extends Enum<E>> implements IString2DataConvertor<E> {

    private Class<E> enumType;

    public String2EnumConvertor(Class<E> clazz) {
        this.enumType = clazz;
    }

    @Override
    public String format(E data, String format) {
        if (data == null) return null;
        // An enum can override toString() method to display user-friendly
        // values
        return data.name();
    }

    @Override
    public E parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

        for (E enumConstant : enumType.getEnumConstants()) {
            if (data.equalsIgnoreCase(enumConstant.name())) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Constant corresponding to value \"%s\" can't be found in Enum %s ", data, enumType.getName()));
    }
}
