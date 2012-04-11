package org.openl.rules.ruleservice.resolver;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenLWrapperRecognizer implements FileSystemWalker.Walker {
    private final Map<String, WSEntryPoint> entryPoints = new HashMap<String, WSEntryPoint>();
    private File baseFolder;

    public OpenLWrapperRecognizer(File baseFolder) {
        this.baseFolder = baseFolder;
    }

    public void process(File file) {
        if (!file.isFile()) {
            return;
        }

        String wsname = getNameWithoutPostfix(file, "Wrapper.java");
        if (wsname != null) {
            if (!entryPoints.containsKey(wsname)) {
                entryPoints.put(wsname, new WSEntryPoint(subtractFilePathes(baseFolder, file), false));
            }
        } else if ((wsname = getNameWithoutPostfix(file, "WrapperInterface.java")) != null) {
            entryPoints.put(wsname, new WSEntryPoint(subtractFilePathes(baseFolder, file), true));
        }
    }
    
    /*internal for test*/ static String subtractFilePathes(File parent, File child) {
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
    
    /*internal for test*/ String getNameWithoutPostfix(File file, String postfix) {
        String filename = file.getName();
        if (filename.endsWith(postfix)) {
            filename = subtractFilePathes(baseFolder, file);
            return filename.substring(0, filename.length() - postfix.length()).replaceAll("[/\\\\]", ".");
        }
        return null;
    }

    public Map<String, WSEntryPoint> getWrappers() {
        return entryPoints;
    }
}
