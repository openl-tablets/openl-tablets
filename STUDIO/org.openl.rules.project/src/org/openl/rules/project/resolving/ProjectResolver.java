package org.openl.rules.project.resolving;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.openl.rules.project.model.ProjectDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author Yury Molchan
 */
public class ProjectResolver {

    private final Logger log = LoggerFactory.getLogger(ProjectResolver.class);
    private List<ResolvingStrategy> resolvingStrategies;

    public static ProjectResolver instance() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "project-resolver-beans.xml");
        return (ProjectResolver) applicationContext.getBean("projectResolver");
    }

    List<ResolvingStrategy> getResolvingStrategies() {
        return resolvingStrategies;
    }

    public void setResolvingStrategies(List<ResolvingStrategy> resolvingStrategies) {
        this.resolvingStrategies = resolvingStrategies;
    }

    /**
     * @param folder Folder to check
     * @return <code>null</code> if it is not OpenL project and
     *         {@link ResolvingStrategy} for this project otherwise.
     */
    public ResolvingStrategy isRulesProject(File folder) {
        if (resolvingStrategies == null) {
            throw new IllegalStateException("Resolving strategies weren't set.");
        }
        for (ResolvingStrategy strategy : resolvingStrategies) {
            if (strategy.isRulesProject(folder)) {
                return strategy;
            }
        }
        return null;
    }

    public ProjectDescriptor resolve(File file) throws ProjectResolvingException {
        ResolvingStrategy strategy = isRulesProject(file);
        if (strategy != null) {
            return strategy.resolveProject(file);
        }
        return null;
    }

    public List<ProjectDescriptor> resolve(File... files) {
        ArrayList<ProjectDescriptor> projectDescriptors = new ArrayList<ProjectDescriptor>();
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
