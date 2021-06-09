package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.openl.rules.project.model.MethodFilter;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ProjectDescriptorValidator;
import org.openl.rules.project.model.validation.ValidationException;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.rits.cloning.Cloner;

public class ProjectDescriptorManager {

    private IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private final ProjectDescriptorValidator validator = new ProjectDescriptorValidator();
    private PathMatcher pathMatcher = new AntPathMatcher();

    private final Cloner cloner = new SafeCloner();

    public PathMatcher getPathMatcher() {
        return pathMatcher;
    }

    public void setPathMatcher(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public IProjectDescriptorSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(IProjectDescriptorSerializer serializer) {
        this.serializer = serializer;
    }

    private ProjectDescriptor readDescriptorInternal(InputStream source) {
        return serializer.deserialize(source);
    }

    public ProjectDescriptor readDescriptor(Path file) throws IOException, ValidationException {
        ProjectDescriptor descriptor;
        try (InputStream inputStream = Files.newInputStream(file)) {
            descriptor = readDescriptorInternal(inputStream);
        }

        postProcess(descriptor, file);
        validator.validate(descriptor);

        return descriptor;
    }

    public ProjectDescriptor readDescriptor(String filename) throws IOException, ValidationException {
        File source = new File(filename);
        return readDescriptor(source.toPath());
    }

    public ProjectDescriptor readOriginalDescriptor(File filename) throws IOException, ValidationException {
        ProjectDescriptor descriptor;
        try (FileInputStream inputStream = new FileInputStream(filename)) {
            descriptor = readDescriptorInternal(inputStream);
        }

        validator.validate(descriptor);

        return descriptor;
    }

    public void writeDescriptor(ProjectDescriptor descriptor, OutputStream dest) throws IOException,
                                                                                 ValidationException {
        validator.validate(descriptor);
        descriptor = cloner.deepClone(descriptor); // prevent changes argument
        // object
        preProcess(descriptor);
        String serializedObject = serializer.serialize(descriptor);
        dest.write(serializedObject.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isModuleWithWildcard(Module module) {
        PathEntry rulesRootPath = module.getRulesRootPath();
        if (rulesRootPath != null) {
            String path = rulesRootPath.getPath();
            return path.contains("*") || path.contains("?");
        }
        return false;
    }

    public boolean isCoveredByWildcardModule(ProjectDescriptor descriptor, Module otherModule) {
        for (Module module : descriptor.getModules()) {
            final PathEntry otherModuleRootPath = otherModule.getRulesRootPath();
            if (isModuleWithWildcard(module) && otherModuleRootPath != null) {
                String relativePath = otherModuleRootPath.getPath().replace("\\", "/");
                if (pathMatcher.match(module.getRulesRootPath().getPath(), relativePath)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<Module> getAllModulesMatchingPathPattern(ProjectDescriptor descriptor,
            Module module,
            String pathPattern) throws IOException {
        List<Module> modules = new ArrayList<>();

        String ptrn = pathPattern.trim();
        Path rootPath = descriptor.getProjectFolder();

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relativePath = rootPath.relativize(file).toString().replace("\\", "/");
                if (isNotTemporaryFile(file) && pathMatcher.match(ptrn, relativePath)) {
                    Path modulePath = file.toAbsolutePath();
                    Module m = new Module();
                    m.setProject(descriptor);
                    m.setRulesRootPath(new PathEntry(relativePath));
                    m.setName(FileUtils.getBaseName(modulePath.toString()));
                    m.setMethodFilter(module.getMethodFilter());
                    m.setWildcardRulesRootPath(pathPattern);
                    m.setWildcardName(module.getName());
                    modules.add(m);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return modules;
    }

    private boolean isNotTemporaryFile(Path file) throws IOException {
        if (file.getFileName().startsWith("~$") || Files.isHidden(file)) {
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

    private boolean containsInProcessedModules(Collection<Module> modules, Module m, Path projectRoot) {
        final Path targetModulePath = projectRoot.resolve(m.getRulesRootPath().getPath());

        for (Module module : modules) {
            Path modulePath = projectRoot.resolve(module.getRulesRootPath().getPath());
            if (targetModulePath.equals(modulePath)) {
                return true;
            }
        }
        return false;
    }

    private void processModulePathPatterns(ProjectDescriptor descriptor, Path projectRoot) throws IOException {
        List<Module> modulesWasRead = descriptor.getModules();
        List<Module> processedModules = new ArrayList<>(modulesWasRead.size());
        // Process modules without wildcard path
        for (Module module : modulesWasRead) {
            if (!isModuleWithWildcard(module)) {
                processedModules.add(module);
            }
        }
        // Process modules with wildcard path
        for (Module module : modulesWasRead) {
            if (isModuleWithWildcard(module)) {
                List<Module> newModules = new ArrayList<>();
                List<Module> modules = getAllModulesMatchingPathPattern(descriptor,
                    module,
                    module.getRulesRootPath().getPath());
                for (Module m : modules) {
                    if (!containsInProcessedModules(processedModules, m, projectRoot)) {
                        newModules.add(m);
                    }
                }
                processedModules.addAll(newModules);
            }
        }

        descriptor.setModules(processedModules);
    }

    private void postProcess(ProjectDescriptor descriptor, Path projectDescriptorFile) throws IOException {
        Path projectRoot = projectDescriptorFile.getParent().toRealPath();
        descriptor.setProjectFolder(projectRoot);
        processModulePathPatterns(descriptor, projectRoot);

        for (Module module : descriptor.getModules()) {
            module.setProject(descriptor);
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

            Path modulePath = Paths.get(module.getRulesRootPath().getPath());
            if (modulePath.isAbsolute()) {
                modulePath = projectRoot.relativize(modulePath);
                PathEntry relativePath = new PathEntry(modulePath.toString());
                module.setRulesRootPath(relativePath);
            }
        }
    }

    private void preProcess(ProjectDescriptor descriptor) {
        // processModulePathPatterns(descriptor);
        if (descriptor.getModules() == null || descriptor.getModules().isEmpty()) {
            return;
        }
        Set<String> wildcardPathSet = new HashSet<>();
        Iterator<Module> itr = descriptor.getModules().iterator();
        while (itr.hasNext()) {
            Module module = itr.next();
            if (module.getWildcardRulesRootPath() == null || !wildcardPathSet
                .contains(module.getWildcardRulesRootPath())) {
                module.setProject(null);
                module.setProperties(null);
                if (module.getWildcardRulesRootPath() != null) {
                    wildcardPathSet.add(module.getWildcardRulesRootPath());
                    module.setRulesRootPath(new PathEntry(module.getWildcardRulesRootPath()));
                    module.setName(module.getWildcardName());
                } else {
                    PathEntry pathEntry = module.getRulesRootPath();
                    String path = pathEntry.getPath();
                    module.setRulesRootPath(new PathEntry(path.replaceAll("\\\\", "/")));
                }
                if (module.getMethodFilter() != null) {
                    boolean f = true;
                    if (module.getMethodFilter().getExcludes() != null && module.getMethodFilter()
                        .getExcludes()
                        .isEmpty()) {
                        module.getMethodFilter().setExcludes(null);
                        f = false;
                    }
                    if (module.getMethodFilter().getIncludes() != null && module.getMethodFilter()
                        .getIncludes()
                        .isEmpty()) {
                        if (f) {
                            module.getMethodFilter().setExcludes(null);
                        } else {
                            module.setMethodFilter(null);
                        }
                    }
                }
            } else {
                itr.remove();
            }
        }
    }

}
