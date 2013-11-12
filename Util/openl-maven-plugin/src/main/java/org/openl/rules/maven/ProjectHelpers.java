package org.openl.rules.maven;

import java.io.File;
import java.util.Arrays;

import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.rules.project.resolving.ResolvingStrategy;
import org.openl.rules.project.resolving.RulesProjectResolver;

final class ProjectHelpers {
    private ProjectHelpers() {
    }

    public static ProjectDescriptor resolveProject(String openlProjectDirectory) throws ProjectResolvingException {
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        File projectFolder = new File(openlProjectDirectory);
        ResolvingStrategy resolvingStrategy = projectResolver.isRulesProject(projectFolder);
        if (resolvingStrategy == null) {
            return null;
        }
        return resolvingStrategy.resolveProject(projectFolder);
    }

    public static IDependencyManager getDependencyManager(ProjectDescriptor projectDescriptor) {
        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();

        dependencyManager.setExecutionMode(false);
        IDependencyLoader loader = new RulesModuleDependencyLoader(projectDescriptor.getModules());
        dependencyManager.setDependencyLoaders(Arrays.asList(loader));

        return dependencyManager;
    }
}
