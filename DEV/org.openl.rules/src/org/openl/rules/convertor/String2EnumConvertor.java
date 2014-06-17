package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

class String2EnumConvertor implements IString2DataConvertor<Enum<?>> {

    private Class<? extends Enum<?>> enumType;

    @SuppressWarnings("unchecked")
    public String2EnumConvertor(Class<? extends Enum<?>> clazz) {
        this.enumType = clazz;
    }

    @Override
    public String format(Enum<?> data, String format) {
        if (data == null) return null;
        // An enum can override toString() method to display user-friendly
        // values
        return data.name();
    }

    ;

    @Override
    public Enum<?> parse(String data, String format, IBindingContext cxt) {
        if (data == null) return null;

        for (Enum<?> enumConstant : enumType.getEnumConstants()) {
            if (data.equalsIgnoreCase(enumConstant.name())) {
                return enumConstant;
            }
        }

        throw new IllegalArgumentException(String.format(
                "Constant corresponding to value \"%s\" can't be found in Enum %s ", data, enumType.getName()));
    }
}
