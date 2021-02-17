package org.openl.rules.project.model;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

public class ProjectDescriptor {
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptor.class);

    private String id;
    private String name;
    private String comment;
    private Path projectFolder;
    private List<Module> modules;
    private List<PathEntry> classpath;

    private List<ProjectDependencyDescriptor> dependencies;
    private String[] propertiesFileNamePatterns;
    private String propertiesFileNameProcessor;

    public String[] getPropertiesFileNamePatterns() {
        return propertiesFileNamePatterns;
    }

    public void setPropertiesFileNamePatterns(String[] propertiesFileNamePatterns) {
        this.propertiesFileNamePatterns = propertiesFileNamePatterns;
    }

    public String getPropertiesFileNameProcessor() {
        return propertiesFileNameProcessor;
    }

    public void setPropertiesFileNameProcessor(String propertiesFileNameProcessor) {
        this.propertiesFileNameProcessor = propertiesFileNameProcessor;
    }

    public List<ProjectDependencyDescriptor> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<ProjectDependencyDescriptor> dependencies) {
        this.dependencies = dependencies;
    }

    public Path getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(Path projectRoot) {
        this.projectFolder = projectRoot;
    }

    /**
     * @deprecated Id is not used anymore. Use {@link #getName()} instead.
     */
    @Deprecated
    public String getId() {
        return id;
    }

    /**
     * @deprecated Id is not used anymore. Use {@link #setName(String)} instead.
     */
    @Deprecated
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<Module> getModules() {
        return modules != null ? modules : new ArrayList<>();
    }

    public void setModules(List<Module> modules) {
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }

    public URL[] getClassPathUrls() {
        if (classpath == null) {
            return new URL[] {};
        }
        URL projectUrl;
        try {
            projectUrl = projectFolder.toUri().normalize().toURL();
        } catch (MalformedURLException e) {
            log.error("Bad URL for the project folder '{}'", projectFolder, e);
            return new URL[] {};
        }
        Set<String> classpaths = processClasspathPathPatterns();
        ArrayList<URL> urls = new ArrayList<>(classpaths.size());

        for (String clspth : classpaths) {
            URL url;
            try {
                // absolute
                url = new URL(clspth);
            } catch (MalformedURLException e1) {
                try {
                    url = new URL(projectUrl, clspth);
                } catch (MalformedURLException e2) {
                    log.error("Bad URL in classpath '{}'", clspth, e2);
                    continue;
                }
            }
            urls.add(url);
        }
        return urls.toArray(new URL[0]);
    }

    private Set<String> processClasspathPathPatterns() {
        Set<String> processedClasspath = new HashSet<>(classpath.size());
        for (PathEntry pathEntry : classpath) {
            String path = pathEntry.getPath().replace('\\', '/').trim();
            if (path.contains("*") || path.contains("?")) {
                check(projectFolder, processedClasspath, path, projectFolder);
            } else {
                // without wildcard path
                if (path.endsWith("/")) {
                    // it is a folder
                    processedClasspath.add(path);
                } else {
                    File file = new File(path);
                    if (file.isAbsolute() && file.isDirectory()) {
                        // it is a folder
                        processedClasspath.add(path + "/");
                    } else if (Files.isDirectory(projectFolder.resolve(path))) {
                        // it is a folder
                        processedClasspath.add(path + "/");
                    } else {
                        // it is a file
                        processedClasspath.add(path);
                    }
                }
            }
        }
        return processedClasspath;
    }

    private void check(Path folder, Collection<String> matched, String pathPattern, Path rootFolder) {
        try {
            Files.walkFileTree(folder, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isDirectory()) {
                        return FileVisitResult.CONTINUE;
                    }
                    String relativePath = rootFolder.relativize(file).toString();
                    relativePath = relativePath.replace('\\', '/');
                    if (new AntPathMatcher().match(pathPattern, relativePath)) {
                        matched.add(relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }
}
