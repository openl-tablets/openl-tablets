package org.openl.util.formatters;

import org.openl.util.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnumFormatter implements IFormatter {

    private final Logger log = LoggerFactory.getLogger(EnumFormatter.class);

    private Class<?> enumClass;

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
