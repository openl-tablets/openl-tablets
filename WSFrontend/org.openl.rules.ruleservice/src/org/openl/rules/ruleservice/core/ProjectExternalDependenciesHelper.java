package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.syntax.code.Dependency;
import org.openl.syntax.code.DependencyType;
import org.openl.syntax.code.IDependency;
import org.openl.syntax.impl.IdentifierNode;

final class ProjectExternalDependenciesHelper {
    private ProjectExternalDependenciesHelper() {
    }

    static String buildDependencyNameForProjectName(String projectName) {
        return "VIRTUAL_MODULE(" + projectName + ")";
    }

    static Map<String, Object> getExternalParamsWithProjectDependencies(Map<String, Object> externalParams,
            Collection<Module> modules) {
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (externalParams != null) {
            parameters.putAll(externalParams);
        }
        if (modules == null) {
            return parameters;
        }
        Set<String> virtualModules = new HashSet<String>();
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
        List<IDependency> externalDependencies = new ArrayList<IDependency>();
        for (String virualModule : virtualModules) {
            externalDependencies.add(new Dependency(DependencyType.MODULE, new IdentifierNode(null,
                null,
                virualModule,
                null)));
        }
        /*Object ed = parameters.get("external-dependencies");
        if (ed != null) {
            @SuppressWarnings("unchecked")
            List<IDependency> dependencies = (List<IDependency>) ed;
            externalDependencies.addAll(dependencies);
        }*/
        //if (!externalDependencies.isEmpty()) {
        parameters.put("external-dependencies", externalDependencies);
        //}
        return parameters;
    }
}
