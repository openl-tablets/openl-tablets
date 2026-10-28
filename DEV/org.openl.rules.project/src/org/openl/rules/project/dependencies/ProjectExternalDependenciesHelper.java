package org.openl.rules.project.dependencies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.dependency.DependencyType;
import org.openl.engine.OpenLCompileManager;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ProjectExternalDependenciesHelper {

    public static Map<String, Object> buildExternalParamsWithProjectDependencies(
            @Nullable Map<String, Object> externalParams,
            ProjectDescriptor projectDescriptor) {
        Objects.requireNonNull(projectDescriptor, "projectDescriptor cannot be null");
        var parameters = new HashMap<String, Object>();
        if (externalParams != null) {
            parameters.putAll(externalParams);
        }
        var externalDependencies = new ArrayList<IDependency>();
        for (var name : autoIncludedProjects(projectDescriptor)) {
            externalDependencies.add(new Dependency(DependencyType.PROJECT, new IdentifierNode(null, null, name, null)));
        }
        parameters.put(OpenLCompileManager.EXTERNAL_DEPENDENCIES_KEY, externalDependencies);
        return parameters;
    }

    /**
     * The names of the projects the project includes whole in its compilation ({@code autoIncluded}), in the order it
     * declares them.
     *
     * <p>A dependency on a Maven artifact declares no name, and its name is {@code null}.
     */
    public static List<String> autoIncludedProjects(ProjectDescriptor projectDescriptor) {
        var dependencies = projectDescriptor.getDependencies();
        if (dependencies == null) {
            return List.of();
        }
        return dependencies.stream()
                .filter(ProjectDependencyDescriptor::isAutoIncluded)
                .map(ProjectDependencyDescriptor::getName)
                .toList();
    }
}
