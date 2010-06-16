package org.openl.rules.webstudio.properties;

import java.text.SimpleDateFormat;
import java.util.*;

import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.xls.formatters.XlsDateFormatter;
import org.openl.rules.webstudio.web.tableeditor.ShowTableBean;

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
    
    /**
     *  
     * @param descriptor
     * @return
     * FIXME: This method is workaround to get system value as string for {@link ShowTableBean#updateSystemProperties()}.
     */
    public String getSystemValueString(String descriptor) {
        String result = null;
        Object resultValue = null;
        ISystemValue systemValue = systemValues.get(descriptor);
        if (systemValue != null) {
            resultValue = systemValue.getValue();
            if(resultValue != null) {
                if (resultValue instanceof Date) {
                    SimpleDateFormat format = new SimpleDateFormat(XlsDateFormatter.DEFAULT_JAVA_DATE_FORMAT);
                    result = format.format((Date)resultValue);                    
                } else {
                    result = resultValue.toString();
                }
            }
        }        
        return result;
    }
}
