package org.openl.rules.project.resolving;

import java.io.File;
import java.util.List;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Detects whether specified folder is OpenL project or not
 * 
 * @author PUdalau
 */
public interface ResolvingStrategy {
    /**
     * @param folder Project root.
     * @return <code>true</code> if specified folder is OpenL project
     */
    boolean isRulesProject(File folder);

    /**
     * @param folder Project root.
     * @return {@link ProjectDescriptor} that describes project
     */
    ProjectDescriptor resolveProject(File folder) throws ProjectResolvingException;

}
