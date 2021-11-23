package org.openl.rules.webstudio.web.util;

import org.springframework.stereotype.Service;

@Service
public class OpenAPIEditorService {

    private OpenAPIEditorService() {
    }

    public static final String DEFAULT_FOLDER = "rules/";
    public static final String DEFAULT_EXTENSION = ".xlsx";

    public String generateModulePath(String moduleName) {
        return DEFAULT_FOLDER + moduleName + DEFAULT_EXTENSION;
    }

}
