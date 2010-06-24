/*
 * Created on Jul 16, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util.tree;

import java.io.File;
import java.util.Iterator;

import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class FileTreeIterator extends TreeIterator<File> {

    public static class FileTreeAdaptor implements TreeIterator.TreeAdaptor<File> {

        public Iterator<File> children(File f) {
            if (!f.isDirectory()) {
                return null;
            }
            return OpenIterator.fromArray(f.listFiles());
        }

    }

    /**
     * @param treeRoot
     * @param adaptor
     * @param mode
     */
    public FileTreeIterator(File root, FileTreeAdaptor adaptor, int mode) {
        super(root, adaptor, mode);
    }

    public FileTreeIterator(File root, int mode) {
        this(root, new FileTreeAdaptor(), mode);
    }

    public File nextFile() {
        return next();
    }

}
