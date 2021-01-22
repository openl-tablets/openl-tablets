package org.openl.rules.project.resolving;

import java.nio.file.Path;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Detects whether specified folder is OpenL project or not.
 *
 * Implementations are loaded using Java SPI.
 *
 * @author PUdalau
 */
public interface ResolvingStrategy {
    /**
     * @param folder Project root.
     * @return <code>true</code> if specified folder is OpenL project
     */
    boolean isRulesProject(Path folder);

    /**
     * @param folder Project root.
     * @return {@link ProjectDescriptor} that describes project
     */
    ProjectDescriptor resolveProject(Path folder) throws ProjectResolvingException;

}
