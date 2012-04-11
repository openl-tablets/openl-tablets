package org.openl.rules.ruleservice.resolver;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * A utility class.
 */
public class FileSystemWalker {
    public interface Walker {
        void process(File file);
    }

    private static final Log log = LogFactory.getLog(FileSystemWalker.class);

    public static String changeExtension(String path, String newExt) {
        int pos = path.lastIndexOf('.');
        if (pos < 0) {
            return path + '.' + newExt;
        }
        return path.substring(0, pos + 1) + newExt;
    }

    public static String removeExtension(String path) {
        int pos = path.lastIndexOf('.');
        if (pos < 0) {
            return path;
        }
        return path.substring(0, pos);
    }

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
}
