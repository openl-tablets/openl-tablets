package org.openl.rules.project.model;

import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.DEPENDENCY_TAG;
import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.PROJECT_DESCRIPTOR_TAG;
import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.PROPERTIES_FILE_NAME_PATTERN;
import static org.openl.rules.project.xml.XmlProjectDescriptorSerializer.PROPERTIES_FILE_NAME_PROCESSOR;

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
import java.util.Objects;
import java.util.Set;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.springframework.util.AntPathMatcher;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.util.FileUtils;
import org.openl.util.RuntimeExceptionWrapper;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = PROJECT_DESCRIPTOR_TAG)
public class ProjectDescriptor {
    @XmlTransient
    private String id;
    @XmlJavaTypeAdapter(XmlProjectDescriptorSerializer.CollapsedStringAdapter2.class)
    private String name;
    private String comment;
    @XmlTransient
    private Path projectFolder;
    @XmlElementWrapper(name = "modules")
    @XmlElement(name = "module")
    private List<Module> modules;
    @XmlElementWrapper(name = "classpath")
    @XmlElement(name = "entry")
    private List<PathEntry> classpath;
    private OpenAPI openapi;

    @XmlElementWrapper(name = "dependencies")
    @XmlElement(name = DEPENDENCY_TAG)
    private List<ProjectDependencyDescriptor> dependencies;
    @XmlElement(name = PROPERTIES_FILE_NAME_PATTERN)
    private String[] propertiesFileNamePatterns;
    @XmlElement(name = PROPERTIES_FILE_NAME_PROCESSOR)
    private String propertiesFileNameProcessor;

    @XmlTransient
    private volatile URL[] classPathUrls;

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

    public String getRelativeUri() {
        Path parent = projectFolder.getParent();
        if (parent == null) {
            return projectFolder.toUri().toString();
        } else {
            return parent.toUri().relativize(projectFolder.toUri()).toString();
        }
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

    private URI fixJarURI(URI jarURI) {
        if ("jar".equals(jarURI.getScheme())) {
            URI uriToZip = jarURI;
            if (uriToZip.getSchemeSpecificPart().contains("%")) {
                // FIXME workaround to fix double URI encoding for URIs from ZipPath
                try {
                    uriToZip = new URI(uriToZip.getScheme() + ":" + uriToZip.getSchemeSpecificPart());
                } catch (URISyntaxException ignored) {
                    // it's ok. let's use original one
                }
            }
            return uriToZip;
        }
        return jarURI;
    }

    public URL[] getClassPathUrls() {
        if (projectFolder == null) {
            return new URL[]{};
        }
        URL projectUrl;
        try {
            projectUrl = fixJarURI(projectFolder.toUri()).normalize().toURL();
            if ("jar".equals(projectUrl.getProtocol())) {
                String file = projectUrl.getPath();
                // jar URLs must be ended with '!/' or '/' for proper URLClassLoader work
                if (!file.endsWith("/")) {
                    String suffix = null;
                    if (file.contains("!/")) {
                        // we are inside jar/zip file like: jar:///file.zip!/project
                        // so needs to add '/' to the end
                        suffix = "/";
                    } else {
                        // projectUrl points to jar/zip file like: jar:///file.zip
                        // so needs to add '!/' to the end
                        suffix = "!/";
                    }
                    projectUrl = new URL(projectUrl.getProtocol(),
                            projectUrl.getHost(),
                            projectUrl.getPort(),
                            projectUrl.getPath() + suffix,
                            null);
                }
            }
        } catch (MalformedURLException e) {
            return new URL[]{};
        }
        if (classpath == null) {
            return new URL[]{projectUrl};
        }
        if (classPathUrls == null) {
            synchronized (this) {
                if (classPathUrls == null) {
                    List<URL> urls = new ArrayList<>();
                    urls.add(projectUrl);
                    List<URL> originalUrls = new ArrayList<>(urls);
                    for (String path : processClasspathPathPatterns()) {
                        path = path.replaceAll("\\\\", "/");
                        URL url;
                        URL originalUrl;
                        try {
                            url = new URL(path.startsWith("/") ? "file://" + path : path).toURI().normalize().toURL();
                            originalUrl = url;
                        } catch (URISyntaxException | MalformedURLException e1) {
                            try {
                                url = new URL(projectUrl.getProtocol(),
                                        projectUrl.getHost(),
                                        projectUrl.getPort(),
                                        projectUrl.getPath() + (projectUrl.getPath().endsWith("/") ? "" : "/") + path,
                                        null).toURI().normalize().toURL();
                                originalUrl = url;
                                // FIXME
                                if ("jar".equals(url.getProtocol()) && "jar".equals(FileUtils.getExtension(path))) {
                                    try {
                                        Path temp = Files.createTempFile("tmp-" + FileUtils.getBaseName(path) + "-",
                                                FileUtils.getExtension(path));
                                        temp.toFile().deleteOnExit();
                                        try (InputStream is = url.openStream()) {
                                            Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
                                        }
                                        url = temp.toUri().normalize().toURL();
                                    } catch (FileNotFoundException ignored) {
                                        // do nothing. It's OK
                                    } catch (IOException e) {
                                        throw RuntimeExceptionWrapper.wrap(e);
                                    }
                                }
                            } catch (URISyntaxException | MalformedURLException e2) {
                                continue;
                            }
                        }
                        boolean f = false;
                        for (URL url1 : originalUrls) {
                            if (url1.sameFile(originalUrl)) {
                                f = true;
                            }
                        }
                        if (!f) {
                            originalUrls.add(originalUrl);
                            urls.add(url);
                        }
                    }
                    classPathUrls = urls.toArray(new URL[0]);
                }
            }
        }
        return classPathUrls;
    }

    private Set<String> processClasspathPathPatterns() {
        Set<String> pathEntries = new HashSet<>();
        for (PathEntry pathEntry : this.classpath) {
            String path = pathEntry.getPath().replace('\\', '/').trim();
            if (path.startsWith("./")) {
                path = path.substring(2);
            }
            if (path.contains("*") || path.contains("?")) {
                resolve(projectFolder, pathEntries, path, projectFolder);
            } else {
                // without wildcard path
                if (path.endsWith("/")) {
                    // it is a folder
                    pathEntries.add(path);
                } else {
                    File file = new File(path);
                    if (file.isAbsolute() && file.isDirectory()) {
                        // it is a folder
                        pathEntries.add(path + "/");
                    } else if (Files.isDirectory(projectFolder.resolve(path))) {
                        // it is a folder
                        pathEntries.add(path + "/");
                    } else {
                        // it is a file
                        pathEntries.add(path);
                    }
                }
            }
        }
        return pathEntries;
    }

    private void resolve(Path folder, Collection<String> pathEntries, String pathPattern, Path rootFolder) {
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
                        pathEntries.add(relativePath);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    public String toString() {
        return "ProjectDescriptor{" + "name='" + name + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ProjectDescriptor that = (ProjectDescriptor) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
