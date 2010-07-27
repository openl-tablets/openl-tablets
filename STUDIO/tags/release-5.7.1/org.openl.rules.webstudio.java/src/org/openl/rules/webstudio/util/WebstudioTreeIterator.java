package org.openl.rules.webstudio.util;

import org.openl.util.OpenIterator;
import org.openl.util.tree.FileTreeIterator;

import java.util.Iterator;
import java.io.File;

public class WebstudioTreeIterator extends FileTreeIterator {
    public static class WebstudioTreeAdaptor extends FileTreeAdaptor {
        public Iterator<File> children(File f) {
            if (!f.isDirectory() || f.getName().equals(PROPERTIES_FOLDER)) {
                return null;
            }
            return OpenIterator.fromArray(f.listFiles());
        }
    }

    public static final String PROPERTIES_FOLDER = ".studioProps";

    public WebstudioTreeIterator(File root, int mode) {
        super(root, mode);
    }
}
