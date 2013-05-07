package org.openl.engine;

import java.util.Map;

import org.openl.util.BooleanUtils;

public class OpenLSystemProperties {

    public static final String CUSTOM_SPREADSHEET_TYPE_PROPERTY = "custom.spreadsheet.type";
    public static final String DISPATCHING_MODE_PROPERTY = "dispatching.mode";
    public static final String DISPATCHING_MODE_JAVA = "java";
    public static final String DISPATCHING_MODE_DT = "dt";

    private OpenLSystemProperties(){}

    public static boolean isJavaDispatchingMode() {
        String dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(DISPATCHING_MODE_JAVA);
    }

    public static boolean isDTDispatchingMode() {
        String dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(DISPATCHING_MODE_DT);
    }

    public static boolean isCustomSpreadsheetType() {
        String customSpreadsheetType = System.getProperty(CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        return BooleanUtils.toBoolean(customSpreadsheetType);
    }

    public static boolean isJavaDispatchingMode(Map<String, Object> externalParameters) {
        String dispatchingMode = null;
        if (externalParameters != null && externalParameters.containsKey(DISPATCHING_MODE_PROPERTY)) {
            dispatchingMode = externalParameters.get(DISPATCHING_MODE_PROPERTY).toString();
        } else {
            dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        }
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(DISPATCHING_MODE_JAVA);
    }

    public static boolean isDTDispatchingMode(Map<String, Object> externalParameters) {
        String dispatchingMode = null;
        if (externalParameters != null && externalParameters.containsKey(DISPATCHING_MODE_PROPERTY)) {
            dispatchingMode = externalParameters.get(DISPATCHING_MODE_PROPERTY).toString();
        } else {
            dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        }
        return dispatchingMode != null && dispatchingMode.equalsIgnoreCase(DISPATCHING_MODE_DT);
    }

    public static String getDispatchingMode(Map<String, Object> externalParameters) {
        String dispatchingMode = null;
        if (externalParameters != null && externalParameters.containsKey(DISPATCHING_MODE_PROPERTY)) {
            dispatchingMode = externalParameters.get(DISPATCHING_MODE_PROPERTY).toString();
        } else {
            dispatchingMode = System.getProperty(DISPATCHING_MODE_PROPERTY);
        }
        return dispatchingMode;
    }

    public static boolean isCustomSpreadsheetType(Map<String, Object> externalParameters) {
        String customSpreadsheetType = null;
        if (externalParameters != null && externalParameters.containsKey(CUSTOM_SPREADSHEET_TYPE_PROPERTY)) {
            customSpreadsheetType = externalParameters.get(CUSTOM_SPREADSHEET_TYPE_PROPERTY).toString();
        } else {
            customSpreadsheetType = System.getProperty(CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        }

        if (customSpreadsheetType == null || customSpreadsheetType.equals("")) {
            return true;
        }

        return BooleanUtils.toBoolean(customSpreadsheetType);
    }
}
