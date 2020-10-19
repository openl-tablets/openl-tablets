package org.openl.rules.webstudio.web.util;

public class OpenAPIEditorUtils {

    private OpenAPIEditorUtils() {
    }

    public static final String DEFAULT_FOLDER = "rules/";
    public static final String DEFAULT_EXTENSION = ".xlsx";
    public static final String DEFAULT_MODELS_PATH = DEFAULT_FOLDER + "Models" + DEFAULT_EXTENSION;
    public static final String DEFAULT_ALGORITHMS_PATH = DEFAULT_FOLDER + "Algorithms" + DEFAULT_EXTENSION;

    public static String generatePath(String moduleName) {
        return DEFAULT_FOLDER + moduleName + DEFAULT_EXTENSION;
    }
}
