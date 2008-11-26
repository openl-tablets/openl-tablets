package org.openl.rules.ruleservice.resolver;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public class OpenLWrapperRecognizer implements FileSystemWalker.Walker {
    private final Map<String, WSEntryPoint> entryPoints;
    private File baseFolder;

    public OpenLWrapperRecognizer(File baseFolder, Map<String, WSEntryPoint> entryPoints) {
        this.baseFolder = baseFolder;
        this.entryPoints = entryPoints;
    }

    public void process(File file) {
        if (!file.isFile())
            return;

        String wsname = getNameWithoutEnding(file, "Wrapper.java");
        if (wsname != null) {
            if (!entryPoints.containsKey(wsname)) {
                entryPoints.put(wsname, new WSEntryPoint(difference(baseFolder, file), false));
            }
        } else if ((wsname = getNameWithoutEnding(file, "WrapperInterface.java")) != null) {
            entryPoints.put(wsname, new WSEntryPoint(difference(baseFolder, file), true));
        }
    }

    String getNameWithoutEnding(File file, String ending) {
        String filename = file.getName();
        if (filename.endsWith(ending)) {
            filename = difference(baseFolder, file);
            return filename.substring(0, filename.length() - ending.length()).replaceAll("[/\\\\]", ".");
        }
        return null;
    }

    static String difference(File parent, File child) {
        try {
            String parentStr = parent.getCanonicalPath();
            String childStr = child.getCanonicalPath();

            if (!childStr.startsWith(parentStr)) {
                return null;
            }

            return childStr.substring(parentStr.length() + 1);
        } catch (IOException e) {
            return null;
        }
    }
}
