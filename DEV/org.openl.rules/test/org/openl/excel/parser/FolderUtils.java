package org.openl.excel.parser;

import java.io.File;
import java.nio.file.Paths;

public final class FolderUtils {
    private FolderUtils() {
    }

    public static String getResourcesFolder() {
        // If run from IDE
        String folder = "test/rules/";

        if (!new File(folder).exists()) {
            // If run jar from the "/target" folder
            String parent = Paths.get("..").toAbsolutePath().normalize().toString();
            folder = parent + "/" + folder;
        }
        if (!new File(folder).exists()) {
            // If run outside of development environment search files in current folder
            folder = "";
        }
        return folder;
    }
}
