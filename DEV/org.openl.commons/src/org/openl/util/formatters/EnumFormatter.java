package org.openl.util.formatters;


import lombok.extern.slf4j.Slf4j;

import org.openl.util.EnumUtils;

@Slf4j
public class EnumFormatter implements IFormatter {


    private final Class<?> enumClass;
    private final Object[] constants;

    public EnumFormatter(Class<?> enumType) {
        this.enumClass = enumType;
        this.constants = EnumUtils.getEnumConstants(enumType);
    }

    @Override
    public String format(Object value) {

        if (!(value instanceof Enum<?>)) {
            log.debug("Should be a {} value: {}", enumClass, value);
            return null;
        }

        return EnumUtils.getName((Enum<?>) value);
    }

    /**
     * The constant of the enumeration the whole text names.
     *
     * <p>The name is matched ignoring case, the way {@link EnumUtils#valueOf} reads it, so a value written by
     * hand in another case is still the value it names.
     */
    @Override
    public Object parse(String value) {
        var constant = EnumUtils.valueOf(constants, value);
        if (constant == null) {
            log.debug("Could not parse {}: {}", enumClass, value);
        }
        return constant;
    }

}
