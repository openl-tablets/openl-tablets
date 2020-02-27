package org.openl.engine;

import java.util.Map;

import org.openl.util.BooleanUtils;

public class OpenLSystemProperties {

    public static final String CUSTOM_SPREADSHEET_TYPE_PROPERTY = "custom.spreadsheet.type";
    public static final String RUN_TESTS_IN_PARALLEL = "test.run.parallel";
    public static final String TEST_RUN_THREAD_COUNT_PROPERTY = "test.run.thread.count";
    public static final String DISPATCHING_MODE_PROPERTY = "dispatching.mode";
    public static final String DISPATCHING_VALIDATION = "dispatching.validation";
    public static final String DISPATCHING_MODE_JAVA = "java";
    public static final String DISPATCHING_MODE_DT = "dt";
    public static final String AUTO_COMPILE = "compile.auto";

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

    public static boolean isRunTestsInParallel(Map<String, Object> externalParameters) {
        String runTestsInParallel = getProperty(externalParameters, RUN_TESTS_IN_PARALLEL);
        return BooleanUtils.toBoolean(runTestsInParallel);
    }

    public static int getTestRunThreadCount(Map<String, Object> externalParameters) {
        String testRunTheadCount = getProperty(externalParameters, TEST_RUN_THREAD_COUNT_PROPERTY);
        return Integer.parseInt(testRunTheadCount);
    }

    public static boolean isCustomSpreadsheetTypesSupported(Map<String, Object> externalParameters) {
        String customSpreadsheetType = getProperty(externalParameters, CUSTOM_SPREADSHEET_TYPE_PROPERTY);
        return BooleanUtils.toBoolean(customSpreadsheetType, true);
    }

    public static boolean isAutoCompile(Map<String, Object> externalParameters) {
        String autoCompile = getProperty(externalParameters, AUTO_COMPILE);
        return BooleanUtils.toBoolean(autoCompile);
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
