package org.openl.rules.project.resolving;

import java.nio.file.Path;
import java.util.ServiceLoader;

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
    public ResolvingStrategy isRulesProject(Path folder) {
        ServiceLoader<ResolvingStrategy> strategies = ServiceLoader.load(ResolvingStrategy.class);

        for (ResolvingStrategy strategy : strategies) {
            if (strategy.isRulesProject(folder)) {
                return strategy;
            }
        }
        return null;
    }

    public ProjectDescriptor resolve(Path file) throws ProjectResolvingException {
        ResolvingStrategy strategy = isRulesProject(file);
        if (strategy != null) {
            return strategy.resolveProject(file);
        }
        return null;
    }
}
