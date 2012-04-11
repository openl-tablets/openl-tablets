package org.openl.rules.table.xls;

import org.openl.rules.table.FormattedCell;
import org.openl.util.Log;

public class XlsBooleanFormat extends XlsFormat{

    @Override
    public String format(Object value) {
        if (!(value instanceof Boolean)) {
            Log.error("Should be Boolean" + value);
            return null;
        }
        Boolean bool = (Boolean) value;
        String fBoolean = bool.toString(); 
        return fBoolean;        
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        
        return null;
    }
    
    public Object parse(String value) {        
        try{
            return new Boolean(value);
        }catch (Exception e) {
            Log.warn("Could not parse Boolean: " + value, e);
            return value;
        }
    }
    
}
