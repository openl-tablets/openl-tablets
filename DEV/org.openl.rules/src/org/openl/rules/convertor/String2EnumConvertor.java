package org.openl.rules.convertor;

import org.openl.util.EnumUtils;

class String2EnumConvertor<E extends Enum<E>> implements IString2DataConvertor<E> {

    private final Class<E> enumType;

    public String2EnumConvertor(Class<E> clazz) {
        this.enumType = clazz;
    }

    @Override
    public E parse(String data, String format) {
        if (data == null) {
            return null;
        }

        var constant = EnumUtils.valueOf(enumType, data);
        if (constant == null) {
            throw new IllegalArgumentException("Constant corresponding to value '%s' cannot be found in Enum %s "
                    .formatted(data, enumType.getName()));
        }
        return enumType.cast(constant);
    }
}
