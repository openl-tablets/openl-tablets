package org.openl.rules.commons.util;

import java.io.FileFilter;
import java.io.File;

public class FoldersOnlyFilter implements FileFilter {
    public boolean accept(File pathname) {
        return pathname.isDirectory();
    }
}
