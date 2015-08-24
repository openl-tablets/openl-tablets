package org.openl.rules.project.resolving;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openl.rules.project.model.Module;

public final class FilenameExtractorUtil {

    private FilenameExtractorUtil() {
    }

    private static Pattern pathPattern = Pattern.compile(".*[^A-Za-z0-9-_,\\s]([A-Za-z0-9-_,\\s]+)\\..*");

    public static String extractFileNameFromModule(Module module) {
        if (module.getRulesRootPath() == null) {
            return module.getName();
        }
        String path = module.getRulesRootPath().getPath();
        Matcher matcher = pathPattern.matcher(path);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return module.getName();
    }
}
