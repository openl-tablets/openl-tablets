package org.openl.util.formatters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.BooleanUtils;

public class BooleanFormatter implements IFormatter {

    private static final Log LOG = LogFactory.getLog(BooleanFormatter.class);

    public String format(Object value) {
        if (!(value instanceof Boolean)) {
            LOG.debug("Should be Boolean: " + value);
            return null;
        }

        Boolean bool = (Boolean) value;
        return bool.toString();
    }

    public Object parse(String value) {
        Boolean boolValue = BooleanUtils.toBooleanObject(value);
        if (boolValue == null) {
            LOG.debug("Could not parse Boolean: " + value);
        }
        return boolValue;
    }
    
}
