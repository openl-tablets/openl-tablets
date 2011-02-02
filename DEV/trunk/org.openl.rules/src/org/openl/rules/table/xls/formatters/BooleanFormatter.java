package org.openl.rules.table.xls.formatters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.util.BooleanUtils;
import org.openl.util.formatters.IFormatter;

public class BooleanFormatter implements IFormatter {

    private static final Log LOG = LogFactory.getLog(BooleanFormatter.class);

    public String format(Object value) {
        if (!(value instanceof Boolean)) {
            LOG.error("Should be Boolean" + value);
            return null;
        }
        Boolean bool = (Boolean) value;
        String fBoolean = bool.toString(); 
        return fBoolean;        
    }

    public Object parse(String value) {
        Boolean boolValue = BooleanUtils.toBooleanObject(value);
        if (boolValue != null) {
            return boolValue;
        } else {
            LOG.warn("Could not parse Boolean: " + value);
            return null;
        }
    }
    
}
