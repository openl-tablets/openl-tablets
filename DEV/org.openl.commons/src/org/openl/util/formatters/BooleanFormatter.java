package org.openl.util.formatters;

import org.openl.util.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanFormatter implements IFormatter {

    private final Logger log = LoggerFactory.getLogger(BooleanFormatter.class);

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
