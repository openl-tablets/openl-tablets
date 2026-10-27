package org.openl.rules.project.resolving;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.openl.rules.project.model.ProjectDescriptor;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author Yury Molchan
 */
public class ProjectResolver {

    private static final ProjectResolver INSTANCE = new ProjectResolver();

    public static ProjectResolver getInstance() {
        return INSTANCE;
    }

    /**
     * @param folder Folder to check
     * @return <code>null</code> if it is not OpenL project and {@link ResolvingStrategy} for this project otherwise.
     */
    public @Nullable ResolvingStrategy isRulesProject(Path folder) {
        var strategies = ServiceLoader.load(ResolvingStrategy.class);

        for (ResolvingStrategy strategy : strategies) {
            if (strategy.isRulesProject(folder)) {
                return strategy;
            }
        }
        return null;
    }

    public @Nullable ProjectDescriptor resolve(Path file) throws ProjectResolvingException {
        return resolve(file, path -> false);
    }

    /**
     * Describes the project in a folder, finding no module in an ignored file or folder of it, such as the output
     * folder of a build.
     *
     * @param file    Folder to check
     * @param ignored tells the files and the folders of the project no module is found in
     * @return <code>null</code> if it is not OpenL project and the description of the project otherwise.
     */
    public @Nullable ProjectDescriptor resolve(Path file, Predicate<Path> ignored) throws ProjectResolvingException {
        var strategy = isRulesProject(file);
        if (strategy != null) {
            return strategy.resolveProject(file, ignored);
        }
        return null;
    }
}
