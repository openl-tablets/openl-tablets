package org.openl.engine;

import java.util.Map;

import org.openl.util.BooleanUtils;

public class OpenLSystemProperties {

    public static final String CUSTOM_SPREADSHEET_TYPE_PROPERTY = "custom.spreadsheet.type";
    public static final String DISPATCHING_VALIDATION = "dispatching.validation";

    private OpenLSystemProperties() {
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
