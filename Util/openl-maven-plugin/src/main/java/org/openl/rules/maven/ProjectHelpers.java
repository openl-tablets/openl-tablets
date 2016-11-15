package org.openl.rules.maven;

import java.io.File;
import java.util.Arrays;

import org.openl.dependency.IDependencyManager;
import org.openl.dependency.loader.IDependencyLoader;
import org.openl.rules.project.dependencies.RulesModuleDependencyLoader;
import org.openl.rules.project.dependencies.RulesProjectDependencyManager;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;

final class ProjectHelpers {
    private ProjectHelpers() {
    }

    public static ProjectDescriptor resolveProject(String openlProjectDirectory) throws ProjectResolvingException {
        ProjectResolver projectResolver = ProjectResolver.instance();
        File projectFolder = new File(openlProjectDirectory);
        return projectResolver.resolve(projectFolder);
    }

    public static IDependencyManager getDependencyManager(ProjectDescriptor projectDescriptor) {
        RulesProjectDependencyManager dependencyManager = new RulesProjectDependencyManager();

        dependencyManager.setExecutionMode(false);
        IDependencyLoader loader = new RulesModuleDependencyLoader(projectDescriptor.getModules());
        dependencyManager.setDependencyLoaders(Arrays.asList(loader));

        return dependencyManager;
    }
}
