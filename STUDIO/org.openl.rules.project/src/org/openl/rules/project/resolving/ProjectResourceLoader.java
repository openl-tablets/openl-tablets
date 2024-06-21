package org.openl.rules.project.resolving;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectResourceLoader {
    private final static ProjectResource[] EMPTY_ARRAY = new ProjectResource[0];

    private final CompiledOpenClass compiledOpenClass;
    private final ProjectDescriptor projectDescriptor;

    public ProjectResourceLoader(ProjectDescriptor projectDescriptor, CompiledOpenClass compiledOpenClass) {
        this.projectDescriptor = Objects.requireNonNull(projectDescriptor, "projectDescriptor cannot be null");
        this.compiledOpenClass = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
    }

    public ProjectResource[] loadResource(String name, boolean includeDependencies) {
        URL[] urls = projectDescriptor.getClassPathUrls();
        if (includeDependencies) {
            ClassLoader classloader = compiledOpenClass.getClassLoader();
            if (classloader instanceof URLClassLoader) {
                URLClassLoader urlClassLoader = (URLClassLoader) classloader;
                urls = urlClassLoader.getURLs();
            }
        }
        List<ProjectResource> projectResources = new ArrayList<>();
        for (URL url : urls) {
            try {
                try (URLClassLoader urlClassLoader1 = new URLClassLoader(new URL[]{url})) {
                    URL resourceURL = urlClassLoader1.getResource(name);
                    if (resourceURL != null) {
                        projectResources.add(new ProjectResource(projectDescriptor, resourceURL));
                    }
                }
            } catch (IOException ignored) {
            }
        }
        return projectResources.toArray(EMPTY_ARRAY);
    }
}
