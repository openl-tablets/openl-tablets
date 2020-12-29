package org.openl.rules.project.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.springframework.util.AntPathMatcher;

public class ProjectDescriptor {
    private String id;
    private String name;
    private String comment;
    private Path projectFolder;
    private List<Module> modules;
    private List<PathEntry> classpath;
    private OpenAPI openapi;

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

    public OpenAPI getOpenapi() {
        return openapi;
    }

    public void setOpenapi(OpenAPI openapi) {
        this.openapi = openapi;
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
        this.modules = modules;
    }

    public List<PathEntry> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<PathEntry> classpath) {
        this.classpath = classpath;
    }

    private URI fixJarURI(URI jarURI) {
        if ("jar".equals(jarURI.getScheme())) {
            URI uriToZip = jarURI;
            if (uriToZip.getSchemeSpecificPart().contains("%")) {
                //FIXME workaround to fix double URI encoding for URIs from ZipPath
                try {
                    uriToZip = new URI(uriToZip.getScheme() + ":" + uriToZip.getSchemeSpecificPart());
                } catch (URISyntaxException ignored) {
                    //it's ok. let's use original one
                }
            }
            return uriToZip;
        }
        return jarURI;
    }

    public URL[] getClassPathUrls() {
        if (projectFolder == null) {
            return new URL[] {};
        }
        URL projectUrl;
        try {
            projectUrl = fixJarURI(projectFolder.toUri()).normalize().toURL();
        } catch (MalformedURLException e) {
            return new URL[] {};
        }
        if (classpath == null) {
            return new URL[] { projectUrl };
        }
        List<URL> urls = new ArrayList<>();
        urls.add(projectUrl);
        for (String path : processClasspathPathPatterns()) {
            URL url;
            try {
                url = new URL(path).toURI().normalize().toURL();
            } catch (URISyntaxException | MalformedURLException e1) {
                try {
                    url = new URL(projectUrl, path).toURI().normalize().toURL();
                    //FIXME
                    if ("jar".equals(url.getProtocol()) && "jar".equals(FileUtils.getExtension(path))) {
                        try {
                            Path temp = Files.createTempFile(FileUtils.getBaseName(path), FileUtils.getExtension(path));
                            temp.toFile().deleteOnExit();
                            try (InputStream is = url.openStream()) {
                                Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
                            }
                            url = temp.toUri().normalize().toURL();
                        } catch (FileNotFoundException ignored) {
                            //do nothing. It's OK
                        } catch (IOException e) {
                            throw RuntimeExceptionWrapper.wrap(e);
                        }
                    }
                } catch (URISyntaxException | MalformedURLException e2) {
                    continue;
                }
            }
            boolean f = false;
            for (URL url1 : urls) {
                if (url1.sameFile(url)) {
                    f = true;
                }
            }
            if (!f) {
                urls.add(url);
            }
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
