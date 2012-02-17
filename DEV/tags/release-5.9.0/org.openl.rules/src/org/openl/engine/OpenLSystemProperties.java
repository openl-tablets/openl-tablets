package org.openl.engine;

import org.openl.util.BooleanUtils;

public class OpenLSystemProperties {
    
    public static final String CUSTOM_SPREADSHEET_TYPE_PROPERTY = "custom.spreadsheet.type";           
    public static final String DISPATCHING_MODE_PROPERTY = "dispatching.mode";
    public static final String DISPATCHING_MODE_JAVA = "java";        
    public static final String DISPATCHING_MODE_DT = "dt";
    
    private OpenLSystemProperties(){}
    
    public static boolean isJavaDispatchingMode() {
        String dispatchingMode = System.getProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY);
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }
    
    public static boolean isDTDispatchingMode() {
        String dispatchingMode = System.getProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY);
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(OpenLSystemProperties.DISPATCHING_MODE_DT);
    }
    
    public static boolean isCustomSpreadsheetType() {
        String customSpreadsheetType = System.getProperty(OpenLSystemProperties.CUSTOM_SPREADSHEET_TYPE_PROPERTY);        
        return BooleanUtils.toBoolean(customSpreadsheetType);
    }
}
