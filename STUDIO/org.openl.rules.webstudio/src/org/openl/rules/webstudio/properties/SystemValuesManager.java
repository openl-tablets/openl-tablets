package org.openl.rules.webstudio.properties;

import java.util.*;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;

/**
 * Manager for system values. Handles implementations for specified system properties.
 * 
 * @author DLiauchuk
 *
 */
public final class SystemValuesManager {

    private Map<String, ISystemValue> systemValues = new HashMap<>();

    private static volatile SystemValuesManager instance;

    public static final String CURRENT_USER_DESCRIPTOR = "currentUser";
    public static final String CURRENT_DATE_DESCRIPTOR = "currentDate";

    private SystemValuesManager() {        
        for (TablePropertyDefinition propDef :TablePropertyDefinitionUtils.getSystemProperties()) {
            if (CURRENT_USER_DESCRIPTOR.equals(propDef.getSystemValueDescriptor())) {
                if (!systemValues.containsKey(CURRENT_USER_DESCRIPTOR)) {
                    systemValues.put(CURRENT_USER_DESCRIPTOR, new CurrentUserValue());
                }                
            } else if (CURRENT_DATE_DESCRIPTOR.equals(propDef.getSystemValueDescriptor())) {
                if (!systemValues.containsKey(CURRENT_DATE_DESCRIPTOR)) {
                    systemValues.put(CURRENT_DATE_DESCRIPTOR, new CurrentDateValue());
                }
            }            
        }
    }

    public static SystemValuesManager getInstance() {
        if (instance == null) { // Double-checked locking
            synchronized (SystemValuesManager.class) {
                if (instance == null) {
                    instance = new SystemValuesManager();
                }
            }
        }
        return instance;
    }

    public Object getSystemValue(String descriptor) {
        Object result = null;
        ISystemValue systemValue = systemValues.get(descriptor);
        if (systemValue != null) {
            result = systemValue.getValue();
        } 
        return result;
    }

}
