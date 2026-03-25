package org.openl.util.formatters;


import lombok.extern.slf4j.Slf4j;

import org.openl.util.EnumUtils;

@Slf4j
public class EnumFormatter implements IFormatter {


    private final Class<?> enumClass;

    public EnumFormatter(Class<?> enumType) {
        this.enumClass = enumType;
    }

    @Override
    public String format(Object value) {

        if (!(value instanceof Enum<?>)) {
            log.debug("Should be a {} value: {}", enumClass, value);
            return null;
        }

        return EnumUtils.getName((Enum<?>) value);
    }

    @Override
    public Object parse(String value) {
        if (value == null) {
            return null;
        }
        return EnumUtils.valueOf(enumClass, value);
    }

}
