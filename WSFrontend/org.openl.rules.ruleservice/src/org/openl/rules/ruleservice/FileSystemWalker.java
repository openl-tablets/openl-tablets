package org.openl.rules.ruleservice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

public class FileSystemWalker {

    private static Log log = LogFactory.getLog(FileSystemWalker.class);

    public static void walk(File root, Walker walker) {
        try {
            walker.process(root);
        } catch (Exception e) {
            log.error("error processing file: " + root, e);
        }

        if (root.isDirectory()) {
            for (File f : root.listFiles()) {
                walk(f, walker);
            }
        }
    }

    interface Walker {
        void process(File file);
    }

    public static boolean deepCopy(File source, File dest, FileFilter filter) {
        File[] files = source.listFiles();
        boolean result = true;
        for (File f : files)
            if (filter.accept(f)) {
                if (f.isDirectory()) {
                    File newDir = new File(dest, f.getName());
                    if (newDir.mkdirs()) {
                        deepCopy(f, newDir, filter);
                    } else {
                        result = false;
                    }
                } else {
                    try {
                        FileCopyUtils.copy(f, new File(dest, f.getName()));
                    } catch (IOException e) {
                        log.error("failed to copy file", e);
                        result = false;
                    }
                }
            }
        return result;
    }

    public static String changeExtension(String path, String newExt) {
        int pos = path.lastIndexOf('.');
        if (pos < 0) return path + '.' + newExt;
        return path.substring(0, pos + 1) + newExt;
    }

    public static String removeExtension(String path) {
        int pos = path.lastIndexOf('.');
        if (pos < 0) return path;
        return path.substring(0, pos);
    }
}
