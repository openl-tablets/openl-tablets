package org.openl.rules.webstudio.properties;

import java.util.*;

import org.openl.rules.table.properties.DefaultPropertyDefinitions;
import org.openl.rules.table.properties.TablePropertyDefinition;

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
        for (TablePropertyDefinition propDef :DefaultPropertyDefinitions.getSystemProperties()) {
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
    
    private static void init() {
        if (instance == null) {
            synchronized (SystemValuesManager.class) {        
                if (instance == null) {
                    instance = new SystemValuesManager();
                }                   
            }
        }        
    }
    
    public static SystemValuesManager instance() {
        init();
        return instance;
    }
    
    public String getSystemValue(String descriptor) {
        String result = null;
        ISystemValue systemValue = systemValues.get(descriptor);
        if (systemValue != null) {
            result = systemValue.getValue();
        } 
        return result;
    }
}
