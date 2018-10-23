package org.openl.rules.project.resolving;

import org.openl.rules.project.model.Module;
import org.openl.util.FileUtils;

public final class FilenameExtractorUtil {

    private FilenameExtractorUtil() {
    }

    public static String extractFileNameFromModule(Module module) {
        if (module.getRulesRootPath() == null) {
            return module.getName();
        }
        String path = module.getRulesRootPath().getPath();
        return FileUtils.getBaseName(path);
    }
}
