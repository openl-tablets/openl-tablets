package org.openl.engine;

import java.util.Map;

import org.openl.util.BooleanUtils;

public class OpenLSystemProperties {

    public static final String CUSTOM_SPREADSHEET_TYPE_PROPERTY = "custom.spreadsheet.type";
    public static final String DISPATCHING_VALIDATION = "dispatching.validation";
    public static final String DISPATCHING_MODE_PROPERTY = "dispatching.mode";
    public static final String DISPATCHING_MODE_JAVA = "java";
    public static final String DISPATCHING_MODE_DT = "dt";

    private OpenLSystemProperties() {
    }

    public static boolean isDTDispatchingMode(Map<String, Object> externalParameters) {
        String dispatchingMode = getProperty(externalParameters, DISPATCHING_MODE_PROPERTY);
        return DISPATCHING_MODE_DT.equalsIgnoreCase(dispatchingMode);
    }

    public static boolean isDispatchingValidationEnabled(Map<String, Object> externalParameters) {
        String dispatchingValidation = getProperty(externalParameters, DISPATCHING_VALIDATION);
        return BooleanUtils.toBoolean(dispatchingValidation);
    }

    public static boolean isCustomSpreadsheetTypesSupported(Map<String, Object> externalParameters) {
        String customSpreadsheetType = getProperty(externalParameters, CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        return BooleanUtils.toBoolean(customSpreadsheetType, true);
    }

    private static String getProperty(Map<String, Object> externalParameters, String property) {
        String value;
        if (externalParameters != null && externalParameters.containsKey(property)) {
            value = externalParameters.get(property).toString();
        } else {
            value = System.getProperty(property);
        }
        return value;
    }
}
