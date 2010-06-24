package org.openl.util.tree;

import java.io.File;
import java.util.Iterator;

import org.openl.util.OpenIterator;

public class FileTreeAdaptor implements TreeIterator.TreeAdaptor<File> {

    public Iterator<File> children(File f) {
        if (!f.isDirectory()) {
            return null;
        }
        return OpenIterator.fromArray(f.listFiles());
    }

}
