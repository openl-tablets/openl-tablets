package org.openl.util.formatters;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.EnumUtils;

public class EnumFormatter implements IFormatter {

    private final Log log = LogFactory.getLog(EnumFormatter.class);

    private Class<?> enumClass;

    public EnumFormatter(Class<?> enumType) {
        this.enumClass = enumType;
    }

    public String format(Object value) {

        if (!(value instanceof Enum<?>)) {

            log.debug(String.format("Should be a %s value: %s", enumClass.toString(), ObjectUtils.toString(value, null)));
            return null;
        }

        return EnumUtils.getName((Enum<?>) value);
    }

    public Object parse(String value) {
        if (value == null) {
            return null;
        }
        return EnumUtils.valueOf(enumClass, value);
    }

}
