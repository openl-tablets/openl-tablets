package org.openl.util.formatters;


import lombok.extern.slf4j.Slf4j;

import org.openl.util.BooleanUtils;

@Slf4j
public class BooleanFormatter implements IFormatter {


    @Override
    public String format(Object value) {
        if (!(value instanceof Boolean)) {
            log.debug("Should be Boolean: {}", value);
            return null;
        }

        Boolean bool = (Boolean) value;
        return bool.toString();
    }

    @Override
    public Object parse(String value) {
        Boolean boolValue = BooleanUtils.toBooleanObject(value);
        if (boolValue == null) {
            log.debug("Could not parse Boolean: {}", value);
        }
        return boolValue;
    }

}
