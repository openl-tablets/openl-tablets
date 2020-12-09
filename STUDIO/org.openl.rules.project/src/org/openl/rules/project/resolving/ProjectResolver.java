package org.openl.rules.project.resolving;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.openl.rules.project.model.ProjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author Yury Molchan
 */
public class ProjectResolver {

    private static final ProjectResolver INSTANCE = new ProjectResolver();
    private final Logger log = LoggerFactory.getLogger(ProjectResolver.class);

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

    public ResolvingStrategy isRulesProject(File folder) {
        return isRulesProject(folder.toPath());
    }

    public ProjectDescriptor resolve(Path file) throws ProjectResolvingException {
        ResolvingStrategy strategy = isRulesProject(file);
        if (strategy != null) {
            return strategy.resolveProject(file);
        }
        return null;
    }

    public ProjectDescriptor resolve(File file) throws ProjectResolvingException {
        return resolve(file.toPath());
    }

    public List<ProjectDescriptor> resolve(File... files) {
        List<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        for (File file : files) {
            try {
                ProjectDescriptor project = resolve(file);
                if (project != null) {
                    projectDescriptors.add(project);
                }
            } catch (Exception ex) {
                log.warn("Failed to resolve project in {}", file, ex);
            }
        }
        return projectDescriptors;
    }
}
