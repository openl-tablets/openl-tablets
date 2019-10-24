package org.openl.rules.project.model;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

public class ProjectDescriptor {
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptor.class);

    private String id;
    private String name;
    private String comment;
    private File projectFolder;
    private List<Module> modules = Collections.emptyList();
    private List<PathEntry> classpath;
    private List<Property> properties;

    private List<ProjectDependencyDescriptor> dependencies;
    private String csrBeansPackage;
    private String propertiesFileNamePattern;
    private String propertiesFileNameProcessor;

    public String getPropertiesFileNamePattern() {
        return propertiesFileNamePattern;
    }

    public void setPropertiesFileNamePattern(String propertiesFileNamePattern) {
        this.propertiesFileNamePattern = propertiesFileNamePattern;
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

    public File getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(File projectRoot) {
        this.projectFolder = projectRoot;
    }

    public String getCsrBeansPackage() {
        return csrBeansPackage;
    }

    public void setCsrBeansPackage(String csrBeansPackage) {
        this.csrBeansPackage = csrBeansPackage;
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
        return modules;
    }

    public void setModules(List<Module> modules) {
        this.modules = modules == null ? Collections.emptyList() : modules;
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
            projectUrl = projectFolder.toURI().toURL();
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
                    } else if (new File(projectFolder, path).isDirectory()) {
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

    private void check(File folder, Collection<String> matched, String pathPattern, File rootFolder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    check(file, matched, pathPattern, rootFolder);
                } else {
                    String relativePath = file.getAbsolutePath().substring(rootFolder.getAbsolutePath().length() + 1);
                    relativePath = relativePath.replace('\\', '/');
                    if (new AntPathMatcher().match(pathPattern, relativePath)) {
                        matched.add(relativePath);
                    }
                }
            }
        }
    }

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }
}
