package org.openl.rules.convertor;

import org.openl.binding.IBindingContext;

public class String2EnumConvertor implements IString2DataConvertor {
    
    private Class<? extends Enum<?>> enumType;

    @SuppressWarnings("unchecked")
    public String2EnumConvertor(Class<?> clazz) {
        assert clazz.isEnum();
        
        this.enumType = (Class<? extends Enum<?>>) clazz;
    }

    public String format(Object data, String format) {
        // An enum can override toString() method to display user-friendly
        // values
        return parse(String.valueOf(data), format, null).toString();
    }

    public Object parse(String data, String format, IBindingContext cxt) {
        Enum<?> resolvedConstant = null;

        for (Enum<?> enumConstant : enumType.getEnumConstants()) {
            if (data.equalsIgnoreCase(enumConstant.name())) {
                resolvedConstant = enumConstant;
                break;
            }
        }

        if (resolvedConstant == null) {
            throw new RuntimeException(String.format(
                    "Constant corresponding to value \"%s\" can't be found in Enum %s ", data, enumType.getName()));
        }

        return resolvedConstant;
    }
}
