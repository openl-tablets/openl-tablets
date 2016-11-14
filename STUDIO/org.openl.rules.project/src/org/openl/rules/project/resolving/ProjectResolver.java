package org.openl.rules.project.resolving;

import java.io.File;
import java.util.List;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Resolves all OpenL projects in specified workspace folder
 *
 * @author PUdalau
 */
public class ProjectResolver {

    private List<ResolvingStrategy> resolvingStrategies;

    public static ProjectResolver instance() {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
            "project-resolver-beans.xml");
        return (ProjectResolver) applicationContext.getBean("projectResolver");
    }

    public List<ResolvingStrategy> getResolvingStrategies() {
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
}
