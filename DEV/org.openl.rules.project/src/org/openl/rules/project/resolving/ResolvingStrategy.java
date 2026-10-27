package org.openl.rules.project.resolving;

import java.nio.file.Path;
import java.util.function.Predicate;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Detects whether specified folder is OpenL project or not.
 * <p>
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

    /**
     * Describes the project as {@link #resolveProject(Path)} does, finding no module in an ignored file or folder.
     *
     * <p>A strategy that finds its modules in the root of the project only takes no such file.
     *
     * @param folder  Project root.
     * @param ignored tells the files and the folders of the project no module is found in
     * @return {@link ProjectDescriptor} that describes project
     */
    default ProjectDescriptor resolveProject(Path folder, Predicate<Path> ignored) throws ProjectResolvingException {
        return resolveProject(folder);
    }
}
