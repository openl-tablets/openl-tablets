package org.openl.rules.project.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openl.dependency.DependencyType;
import org.openl.engine.OpenLCompileManager;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

public final class ProjectExternalDependenciesHelper {

    private ProjectExternalDependenciesHelper() {
    }

    public static Map<String, Object> buildExternalParamsWithProjectDependencies(Map<String, Object> externalParams,
                                                                                 ProjectDescriptor projectDescriptor) {
        Objects.requireNonNull(projectDescriptor, "projectDescriptor cannot be null");
        Map<String, Object> parameters = new HashMap<>();
        if (externalParams != null) {
            parameters.putAll(externalParams);
        }
        List<IDependency> externalDependencies = new ArrayList<>();
        if (projectDescriptor.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependency : projectDescriptor.getDependencies()) {
                if (dependency.isAutoIncluded()) {
                    externalDependencies.add(new Dependency(DependencyType.PROJECT,
                            new IdentifierNode(null, null, dependency.getName(), null)));
                }
            }
        }
        parameters.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, externalDependencies);
        return parameters;
    }
}
