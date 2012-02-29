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
public class SystemValuesManager {
    
    private Map<String, ISystemValue> systemValues = new HashMap<String, ISystemValue>();
    
    private static volatile SystemValuesManager instance;
    
    private final String currentUserDescription = "currentUser";
    private final String currentDatedescription = "currentDate";
    
    private SystemValuesManager() {        
        for (TablePropertyDefinition propDef :TablePropertyDefinitionUtils.getSystemProperties()) {
            if (currentUserDescription.equals(propDef.getSystemValueDescriptor())) {
                if (!systemValues.containsKey(currentUserDescription)) {
                    systemValues.put(currentUserDescription, new CurrentUserValue());
                }                
            } else if (currentDatedescription.equals(propDef.getSystemValueDescriptor())) {
                if (!systemValues.containsKey(currentDatedescription)) {
                    systemValues.put(currentDatedescription, new CurrentDateValue());
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
