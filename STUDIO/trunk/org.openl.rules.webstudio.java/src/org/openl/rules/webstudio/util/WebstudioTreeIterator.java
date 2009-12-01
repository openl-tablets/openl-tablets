package org.openl.rules.webstudio.util;

import org.openl.util.OpenIterator;
import org.openl.util.tree.TreeIterator;

import java.util.Iterator;
import java.io.File;

public class WebstudioTreeIterator extends TreeIterator<File> {
    static class TreeAdaptor implements TreeIterator.TreeAdaptor<File> {
        public Iterator<File> children(File f) {
            if (!f.isDirectory() || f.getName().equals(PROPERTIES_FOLDER)) {
                return null;
            }
            return OpenIterator.fromArray(f.listFiles());
        }
    }

    public static final String PROPERTIES_FOLDER = ".studioProps";

    public WebstudioTreeIterator(File treeRoot, int mode) {
        super(treeRoot, new TreeAdaptor(), mode);
    }

    public File nextFile() {
        return next();
    }

}
