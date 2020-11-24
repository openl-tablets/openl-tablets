package org.openl.rules.webstudio.web.util;

import org.openl.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class OpenAPIEditorService {

    private OpenAPIEditorService() {
    }

    public static final String DEFAULT_FOLDER = "rules/";
    public static final String DEFAULT_EXTENSION = ".xlsx";

    public String generatePath(String moduleName) {
        return DEFAULT_FOLDER + moduleName + DEFAULT_EXTENSION;
    }

    public String getDisplayName(String mode) {
        if (StringUtils.isBlank(mode)) {
            return "";
        }
        return StringUtils.capitalize(mode.toLowerCase());
    }
}
