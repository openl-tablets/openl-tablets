package org.openl.rules.project.resolving;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openl.CompiledOpenClass;
import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectResourceLoader {
    private final CompiledOpenClass compiledOpenClass;

    private final static ProjectResource[] EMPTY_ARRAY = new ProjectResource[0];

    public ProjectResourceLoader(CompiledOpenClass compiledOpenClass) {
        this.compiledOpenClass = Objects.requireNonNull(compiledOpenClass, "compiledOpenClass cannot be null");
    }

    public ProjectResource[] loadResource(String name) {
        ClassLoader classloader = compiledOpenClass.getClassLoader();
        if (classloader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classloader;
            URL[] urls = urlClassLoader.getURLs();
            List<ProjectResource> projectResources = new ArrayList<>();
            for (URL url : urls) {
                String filePath;
                try {
                    filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException ignored) {
                    filePath = url.getFile();
                }
                File projectFolder = new File(filePath);
                ResolvingStrategy resolvingStrategy = ProjectResolver.getInstance().isRulesProject(projectFolder);
                if (resolvingStrategy != null) {
                    try {
                        ProjectDescriptor projectDescriptor = resolvingStrategy.resolveProject(projectFolder);
                        URLClassLoader urlClassLoader1 = new URLClassLoader(new URL[] { url });
                        URL resourceURL = urlClassLoader1.getResource(name);
                        if (resourceURL != null) {
                            projectResources.add(new ProjectResource(projectDescriptor, resourceURL));
                        }
                    } catch (ProjectResolvingException ignored) {
                    }
                }
            }
            return projectResources.toArray(EMPTY_ARRAY);
        }
        return EMPTY_ARRAY;
    }
}
