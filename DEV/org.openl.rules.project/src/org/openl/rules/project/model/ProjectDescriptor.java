package org.openl.rules.project.model;

import static org.openl.rules.project.model.ProjectDependencyDescriptor.DEPENDENCY_TAG;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.OS;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringUtils;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "project")
@Getter
@Setter
@Slf4j
public class ProjectDescriptor {
    @XmlJavaTypeAdapter(CollapsedStringAdapter2.class)
    private String name;
    private String comment;
    @XmlTransient
    private Path projectFolder;
    @XmlElement(name = "modules")
    @XmlJavaTypeAdapter(ModulesAdapter.class)
    private List<Module> modules = new ArrayList<>();
    @XmlElement(name = "classpath")
    @XmlJavaTypeAdapter(ClasspathAdapter.class)
    private List<String> classpath = new ArrayList<>();
    private OpenAPI openapi;

    @XmlElementWrapper(name = "dependencies")
    @XmlElement(name = DEPENDENCY_TAG)
    private List<ProjectDependencyDescriptor> dependencies;
    @XmlElement(name = "properties-file-name-pattern")
    private String[] propertiesFileNamePatterns;
    @XmlElement(name = "properties-file-name-processor")
    private String propertiesFileNameProcessor;

    /**
     * Project-level filter for controlling which methods are included in the generated service interface.
     * <p>
     * Unlike module-level filters which use regex patterns against full method signatures,
     * this filter uses glob-style patterns ({@code *} and {@code ?}) matched against method names only,
     * making it simpler for users to configure.
     */
    @XmlElement(name = "exposed-methods")
    private ExposedMethods exposedMethods;

    /** Lazy classpath URL cache; computed by {@link #getClassPathUrls()}, not exposed via Lombok. */
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @XmlTransient
    private volatile URL[] classPathUrls;

    public String getRelativeUri() {
        Path parent = projectFolder.getParent();
        if (parent == null) {
            return projectFolder.toUri().toString();
        } else {
            return parent.toUri().relativize(projectFolder.toUri()).toString();
        }
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

    private static final List<String> DEFAULT_CLASSPATH = List.of("groovy/", "lib/*.jar");

    private Set<String> processClasspathPathPatterns() {
        Set<String> pathEntries = new HashSet<>();
        var entries = CollectionUtils.isEmpty(classpath) ? DEFAULT_CLASSPATH : this.classpath;
        for (String pathEntry : entries) {
            String path = pathEntry.replace('\\', '/').trim();
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
                    if (FileUtils.pathMatches(pathPattern, relativePath)) {
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
        if (!(o instanceof ProjectDescriptor that))
            return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static final String FILE_NAME = "rules.xml";
    private static final JAXBSerializer SERIALIZER = new JAXBSerializer(ProjectDescriptor.class);

    public ProjectDescriptor expand() throws IOException {
        fillProjectName();
        fillModulesByPattern();

        for (Module module : getModules()) {
            module.setProject(this);
            if (module.getMethodFilter() == null) {
                module.setMethodFilter(new MethodFilter());
            }
            if (module.getMethodFilter().getExcludes() == null) {
                module.getMethodFilter().setExcludes(new HashSet<>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getExcludes().removeAll(Arrays.asList("", null));
            }

            if (module.getMethodFilter().getIncludes() == null) {
                module.getMethodFilter().setIncludes(new HashSet<>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getIncludes().removeAll(Arrays.asList("", null));
            }

            if (StringUtils.isBlank(module.getRulesRootPath())) {
                continue;
            }
            var path = module.getRulesRootPath();
            if (StringUtils.isBlank(module.getName())) {
                module.setName(FileUtils.getBaseName(path));
            }
            Path modulePath = Path.of(path);
            if (modulePath.isAbsolute()) {
                modulePath = projectFolder.relativize(modulePath);
                module.setRulesRootPath(modulePath.toString());
            }
        }
        return this;
    }

    private void fillModulesByPattern() throws IOException {
        var readModules = getModules();
        if (readModules.isEmpty()) {
            var rules = new Module();
            rules.setRulesRootPath("rules/**/*.xlsx");
            var tests = new Module();
            tests.setRulesRootPath("tests/**/*.xlsx");
            readModules = Arrays.asList(rules, tests);
        }
        var processedModules = new ArrayList<Module>(readModules.size());
        // Process modules without wildcard path
        for (var module : readModules) {
            if (!module.isModuleWithWildcard()) {
                processedModules.add(module);
            }
        }
        // Process modules with wildcard path
        for (Module module : readModules) {
            if (module.isModuleWithWildcard()) {
                var newModules = new ArrayList<Module>();
                var matchedModules = getAllModulesMatchingPathPattern(module, module.getRulesRootPath());
                for (var m : matchedModules) {
                    if (!containsInProcessedModules(processedModules, m, getProjectFolder())) {
                        newModules.add(m);
                    }
                }
                processedModules.addAll(newModules);
            }
        }

        setModules(processedModules);
    }

    private void fillProjectName() {
        if (StringUtils.isNotBlank(getName())) {
            return;
        }
        var projectFolderName = getProjectFolder().getFileName();
        if (projectFolderName != null) {
            setName(projectFolderName.toString());
        } else {
            var path = getProjectFolder().toUri().getSchemeSpecificPart();
            var zipName = FileUtils.getBaseName(path.substring(0, path.length() - 2));
            setName(zipName);
        }
    }

    private boolean containsInProcessedModules(Collection<Module> modules, Module m, Path projectRoot) {
        final Path targetModulePath = projectRoot.resolve(m.getRulesRootPath());

        for (Module module : modules) {
            Path modulePath = projectRoot.resolve(module.getRulesRootPath());
            if (targetModulePath.equals(modulePath)) {
                return true;
            }
        }
        return false;
    }

    public List<Module> getAllModulesMatchingPathPattern(Module module,
                                                         String pathPattern) throws IOException {
        List<Module> matchedModules = new ArrayList<>();

        String ptrn = pathPattern.trim();
        Path rootPath = getProjectFolder();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = rootPath.relativize(file).toString().replace("\\", "/");
                if (isNotTemporaryFile(file) && FileUtils.pathMatches(ptrn, relativePath)) {
                    Path modulePath = file.toAbsolutePath();
                    Module m = new Module();
                    m.setProject(ProjectDescriptor.this);
                    m.setRulesRootPath(relativePath);
                    m.setName(FileUtils.getBaseName(modulePath.toString()));
                    m.setMethodFilter(module.getMethodFilter());
                    if (module.getWebstudioConfiguration() != null) {
                        WebstudioConfiguration webstudioConfiguration = new WebstudioConfiguration();
                        webstudioConfiguration
                                .setCompileThisModuleOnly(module.getWebstudioConfiguration().isCompileThisModuleOnly());
                        m.setWebstudioConfiguration(webstudioConfiguration);
                    }
                    m.setWildcardRulesRootPath(pathPattern);
                    m.setWildcardName(module.getName());
                    matchedModules.add(m);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return matchedModules;
    }

    private boolean isNotTemporaryFile(Path file) throws IOException {
        if (file.getFileName().startsWith("~$")) {
            return false;
        }
        if (OS.isWindows() && Files.isHidden(file)) {
            OutputStream os = null;
            try {
                os = Files.newOutputStream(file, StandardOpenOption.APPEND);
            } catch (Exception unused) {
                return false;
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
        return true;
    }

    public static ProjectDescriptor read(Path path) {
        var file = Files.isDirectory(path) ? path.resolve(FILE_NAME) : path;
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (var in = Files.newInputStream(file)) {
            var descriptor = read(in);
            if (descriptor != null) {
                descriptor.setProjectFolder(file.getParent());
            }
            return descriptor;
        } catch (IOException e) {
            log.warn("Failed to read '{}'.", file, e);
            return null;
        }
    }

    public static ProjectDescriptor read(InputStream in) {
        try {
            return (ProjectDescriptor) SERIALIZER.unmarshal(in);
        } catch (JAXBException e) {
            log.warn("Failed to parse '{}'.", FILE_NAME, e);
            return null;
        }
    }

    public byte[] toBytes() {
        var outputStream = new ByteArrayOutputStream();
        try {
            SERIALIZER.marshal(this, outputStream);
        } catch (JAXBException e) {
            throw new IllegalStateException(e);
        }
        return outputStream.toByteArray();
    }

    @SuppressWarnings("unused")
    private void beforeMarshal(Marshaller marshaller) {
        name = StringUtils.trimToNull(name);
        comment = StringUtils.trimToNull(comment);
        propertiesFileNameProcessor = StringUtils.trimToNull(propertiesFileNameProcessor);
        if (propertiesFileNamePatterns != null) {
            var trimmed = Arrays.stream(propertiesFileNamePatterns)
                    .filter(StringUtils::isNotBlank)
                    .toArray(String[]::new);
            propertiesFileNamePatterns = trimmed.length == 0 ? null : trimmed;
        }
        if (classpath != null) {
            classpath.replaceAll(StringUtils::trimToNull);
            classpath.removeIf(Objects::isNull);
            if (classpath.isEmpty()) {
                classpath = null;
            }
        }
        if (dependencies != null && dependencies.isEmpty()) {
            dependencies = null;
        }
        if (OpenAPI.isEmpty(openapi) || OpenAPI.isDefault(openapi)) {
            openapi = null;
        }
        if (ExposedMethods.isEmpty(exposedMethods)) {
            exposedMethods = null;
        }
        dropModulesWithoutRulesRootPath();
    }

    private void dropModulesWithoutRulesRootPath() {
        if (modules != null) {
            modules.removeIf(m -> StringUtils.isBlank(m.getRulesRootPath()));
        }
    }

    /**
     * Wrapper holding the inner {@code <module>} elements of the {@code <modules>} block. Defined solely
     * so {@link ModulesAdapter} can convert between the wrapper-shape JAXB expects on the wire and the
     * plain {@code List<Module>} the model exposes.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ModulesXml {
        @XmlElement(name = "module")
        List<Module> module;
    }

    /**
     * Maps an empty {@code <modules>} block to a {@code null} marshal result so JAXB omits the wrapper
     * entirely; conversely, a missing block unmarshals to an empty {@link ArrayList} so consumers never
     * see {@code null}.
     */
    static class ModulesAdapter extends XmlAdapter<ModulesXml, List<Module>> {
        @Override
        public List<Module> unmarshal(ModulesXml v) {
            return (v == null || v.module == null) ? new ArrayList<>() : v.module;
        }

        @Override
        public ModulesXml marshal(List<Module> v) {
            if (v == null || v.isEmpty()) {
                return null;
            }
            var wrapper = new ModulesXml();
            wrapper.module = v;
            return wrapper;
        }
    }

    /**
     * Wrapper holding a single {@code <entry path="..."/>} element of the {@code <classpath>} block;
     * the {@code path} attribute is the wire shape consumed by {@link ClasspathAdapter}.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ClasspathEntryXml {
        @XmlAttribute
        String path;
    }

    /**
     * Wrapper holding the inner {@code <entry>} elements of the {@code <classpath>} block. See
     * {@link ClasspathAdapter} for the role this plays.
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    static class ClasspathXml {
        @XmlElement(name = "entry")
        List<ClasspathEntryXml> entry;
    }

    /**
     * Same null/empty contract as {@link ModulesAdapter} but for the {@code <classpath>} block.
     */
    static class ClasspathAdapter extends XmlAdapter<ClasspathXml, List<String>> {
        @Override
        public List<String> unmarshal(ClasspathXml v) {
            if (v == null || v.entry == null) {
                return new ArrayList<>();
            }
            var result = new ArrayList<String>(v.entry.size());
            for (var e : v.entry) {
                var path = StringUtils.trimToNull(e.path);
                if (path != null) {
                    result.add(path);
                }
            }
            return result;
        }

        @Override
        public ClasspathXml marshal(List<String> v) {
            if (v == null || v.isEmpty()) {
                return null;
            }
            var wrapper = new ClasspathXml();
            wrapper.entry = new ArrayList<>(v.size());
            for (var path : v) {
                var entry = new ClasspathEntryXml();
                entry.path = path;
                wrapper.entry.add(entry);
            }
            return wrapper;
        }
    }
}
