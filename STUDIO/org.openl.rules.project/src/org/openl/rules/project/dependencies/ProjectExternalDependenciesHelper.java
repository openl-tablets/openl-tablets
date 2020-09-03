package org.openl.rules.project.dependencies;

import java.util.*;

import org.openl.engine.OpenLCompileManager;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public final class ProjectExternalDependenciesHelper {

    public static final String VIRTUAL_MODULE_PREFIX = "VIRTUAL_MODULE(";
    public static final String VIRTUAL_MODULE_SUFFIX = ")";

    private ProjectExternalDependenciesHelper() {
    }

    public static String buildDependencyNameForProject(String projectName) {
        return VIRTUAL_MODULE_PREFIX + projectName + VIRTUAL_MODULE_SUFFIX;
    }

    public static Map<String, Object> getExternalParamsWithProjectDependencies(Map<String, Object> externalParams,
            Collection<Module> modules) {
        Map<String, Object> parameters = new HashMap<>();
        if (externalParams != null) {
            parameters.putAll(externalParams);
        }
        if (modules == null) {
            parameters.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, null);
            return parameters;
        }
        Set<String> virtualModules = new HashSet<>();
        for (Module module : modules) {
            ProjectDescriptor projectDescriptor = module.getProject();
            if (projectDescriptor.getDependencies() != null) {
                for (ProjectDependencyDescriptor dependency : projectDescriptor.getDependencies()) {
                    if (dependency.isAutoIncluded()) {
                        virtualModules.add(buildDependencyNameForProject(dependency.getName()));
                    }
                }
            }
        }
        List<IDependency> externalDependencies = new ArrayList<>();
        for (String virtualModule : virtualModules) {
            externalDependencies
                .add(new Dependency(DependencyType.MODULE, new IdentifierNode(null, null, virtualModule, null)));
        }

        parameters.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, externalDependencies);

        return parameters;
    }
}
