package org.openl.rules.webstudio.properties;

import java.util.Calendar;

/**
 * Handles current date value of the system.
 * 
 * @author DLiauchuk
 *
 */
public class CurrentDateValue implements ISystemValue {
    
    @Override
    public Object getValue() {
        Calendar cal = Calendar.getInstance();        
        return cal.getTime(); 
    }

}
