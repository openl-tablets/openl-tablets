package org.openl.rules.project.resolving;

import java.io.File;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.util.tree.FileTreeIterator.FileTreeAdaptor;

/**
 * Detects whether specified folder is OpenL project or not
 * 
 * @author PUdalau
 */
public interface ResolvingStrategy {
    /**
     * @param folder Project root.
     * @param fileTreeAdaptor {@link FileTreeAdaptor} that have to be used for
     *            file search inside the project to determine which
     *            files/folders have to be used in search.
     * @return <code>true</code> if specified folder is OpenL project
     */
    boolean isRulesProject(File folder, FileTreeAdaptor fileTreeAdaptor);

    /**
     * @param folder Project root.
     * @param fileTreeAdaptor {@link FileTreeAdaptor} that have to be used for
     *            file search inside the project to determine which
     *            files/folders have to be used in search.
     * @return {@link ProjectDescriptor} that describes project
     */
    ProjectDescriptor resolveProject(File folder, FileTreeAdaptor fileTreeAdaptor);
}
