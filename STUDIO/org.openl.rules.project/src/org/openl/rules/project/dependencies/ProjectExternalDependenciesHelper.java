package org.openl.rules.project.dependencies;

import java.util.*;

import org.openl.engine.OpenLSourceManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public final class ProjectExternalDependenciesHelper {
    private ProjectExternalDependenciesHelper() {
    }

    public static String buildDependencyNameForProjectName(String projectName) {
        return "VIRTUAL_MODULE(" + projectName + ")";
    }

    public static boolean isProject(String dependencyName) {
        return dependencyName.indexOf("VIRTUAL_MODULE(") == 0 && dependencyName
            .lastIndexOf(')') == dependencyName.length() - 1;
    }

    public static String getProjectName(String dependencyName) {
        if (isProject(dependencyName)) {
            return dependencyName.substring("VIRTUAL_MODULE(".length(), dependencyName.length() - 1);
        }
        return null;
    }

    public static Map<String, Object> getExternalParamsWithProjectDependencies(Map<String, Object> externalParams,
            Collection<Module> modules) {
        Map<String, Object> parameters = new HashMap<>();
        if (externalParams != null) {
            parameters.putAll(externalParams);
        }
        if (modules == null) {
            parameters.put(OpenLSourceManager.EXTERNAL_DEPENDENCIES_KEY, null);
            return parameters;
        }
        Set<String> virtualModules = new HashSet<>();
        for (Module module : modules) {
            ProjectDescriptor projectDescriptor = module.getProject();
            if (projectDescriptor.getDependencies() != null) {
                for (ProjectDependencyDescriptor dependency : projectDescriptor.getDependencies()) {
                    if (dependency.isAutoIncluded()) {
                        virtualModules.add(buildDependencyNameForProjectName(dependency.getName()));
                    }
                }
            }
        }
        List<IDependency> externalDependencies = new ArrayList<>();
        for (String virualModule : virtualModules) {
            externalDependencies
                .add(new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, virualModule, null)));
        }

        parameters.put(OpenLSourceManager.EXTERNAL_DEPENDENCIES_KEY, externalDependencies);

        return parameters;
    }
}
