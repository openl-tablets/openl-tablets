package org.openl.rules.project.dependencies;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;

/**
 * @author Yury Molchan
 */
class ProjectExternalDependenciesHelperTest {

    /** A dependency on a Maven artifact names no project, so it includes none whatever its flag says. */
    @Test
    void includesNoProjectForALibrary() {
        var project = new ProjectDescriptor();
        project.setDependencies(List.of(dependency("Domain", true),
                dependency(null, true),
                dependency("Rates", false)));

        assertEquals(List.of("Domain"), ProjectExternalDependenciesHelper.autoIncludedProjects(project));
    }

    private static ProjectDependencyDescriptor dependency(String name, boolean autoIncluded) {
        var dependency = new ProjectDependencyDescriptor();
        dependency.setName(name);
        dependency.setAutoIncluded(autoIncluded);
        if (name == null) {
            dependency.setMavenArtifact("com.example:lib:jar:1.0.0");
        }
        return dependency;
    }
}
