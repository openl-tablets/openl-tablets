package org.openl.rules.project;

import java.io.*;
import java.util.*;

import com.rits.cloning.Cloner;
import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.extension.instantiation.ExtensionDescriptorFactory;
import org.openl.rules.extension.instantiation.IExtensionDescriptor;
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

public class ProjectDescriptorManager {

    private IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private ProjectDescriptorValidator validator = new ProjectDescriptorValidator();
    private PathMatcher pathMatcher = new AntPathMatcher();

    private Cloner cloner = new SafeCloner();

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

    public ProjectDescriptor readDescriptor(File file) throws IOException, ValidationException {
        FileInputStream inputStream = new FileInputStream(file);

        ProjectDescriptor descriptor = readDescriptorInternal(inputStream);
        IOUtils.closeQuietly(inputStream);

        postProcess(descriptor, file);
        validator.validate(descriptor);

        return descriptor;
    }

    public ProjectDescriptor readDescriptor(String filename) throws IOException, ValidationException {
        File source = new File(filename);
        return readDescriptor(source);
    }

    public ProjectDescriptor readOriginalDescriptor(File filename) throws FileNotFoundException, ValidationException {
        FileInputStream inputStream = new FileInputStream(filename);

        ProjectDescriptor descriptor = readDescriptorInternal(inputStream);
        IOUtils.closeQuietly(inputStream);

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
        dest.write(serializedObject.getBytes("UTF-8"));
    }

    public boolean isModuleWithWildcard(Module module) {
        PathEntry rulesRootPath = module.getRulesRootPath();
        if (rulesRootPath != null) {
            String path = rulesRootPath.getPath();
            return path.contains("*") || path.contains("?");
        }
        return false;
    }

    private void check(File folder, List<File> matched, String pathPattern, File rootFolder) {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                check(file, matched, pathPattern, rootFolder);
            } else {
                String relativePath = file.getAbsolutePath().substring(rootFolder.getAbsolutePath().length() + 1);
                relativePath = relativePath.replace("\\", "/");
                if (pathMatcher.match(pathPattern, relativePath)) {
                    matched.add(file);
                }
            }
        }
    }

    public List<Module> getAllModulesMatchingPathPattern(ProjectDescriptor descriptor,
            Module module,
            String pathPattern) throws IOException {
        List<Module> modules = new ArrayList<>();

        List<File> files = new ArrayList<>();
        check(descriptor.getProjectFolder(), files, pathPattern.trim(), descriptor.getProjectFolder());

        for (File file : files) {
            if (isTemporaryFile(file)) {
                continue;
            }
            Module m = new Module();
            m.setProject(descriptor);
            m.setRulesRootPath(new PathEntry(file.getCanonicalPath()));
            m.setName(FileUtils.getBaseName(file.getName()));
            m.setMethodFilter(module.getMethodFilter());
            m.setWildcardRulesRootPath(pathPattern);
            m.setWildcardName(module.getName());
            m.setExtension(module.getExtension());
            modules.add(m);
        }
        return modules;
    }

    private boolean isTemporaryFile(File file) {
        if (file.getName().startsWith("~$") && file.isHidden()) {
            OutputStream os = null;
            try {
                os = new FileOutputStream(file, true);
            } catch (FileNotFoundException unused) {
                return true;
            } finally {
                IOUtils.closeQuietly(os);
            }
        }
        return false;
    }

    private boolean containsInProcessedModules(Collection<Module> modules, Module m, File projectRoot) {
        PathEntry pathEntry = m.getRulesRootPath();
        if (!new File(m.getRulesRootPath().getPath()).isAbsolute()) {
            pathEntry = new PathEntry(new File(projectRoot, m.getRulesRootPath().getPath()).getAbsolutePath());
        }

        for (Module module : modules) {
            PathEntry modulePathEntry = module.getRulesRootPath();
            if (!new File(module.getRulesRootPath().getPath()).isAbsolute()) {
                modulePathEntry = new PathEntry(
                    new File(projectRoot, module.getRulesRootPath().getPath()).getAbsolutePath());
            }
            if (pathEntry.getPath().equals(modulePathEntry.getPath())) {
                return true;
            }
        }
        return false;
    }

    private void processModulePathPatterns(ProjectDescriptor descriptor, File projectRoot) throws IOException {
        List<Module> modulesWasRead = descriptor.getModules();
        List<Module> processedModules = new ArrayList<>(modulesWasRead.size());
        // Process modules without wildcard path
        for (Module module : modulesWasRead) {
            if (!isModuleWithWildcard(module) && module.getExtension() == null) {
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
        // Process extension modules
        for (Module module : modulesWasRead) {
            if (module.getExtension() != null) {
                ClassLoader classLoader = new SimpleBundleClassLoader(Thread.currentThread().getContextClassLoader());
                IExtensionDescriptor extensionDescriptor = ExtensionDescriptorFactory
                    .getExtensionDescriptor(module.getExtension(), classLoader);
                module.setProject(descriptor);
                processedModules.addAll(extensionDescriptor.getInternalModules(module));
                ClassLoaderUtils.close(classLoader);
            }
        }

        descriptor.setModules(processedModules);
    }

    private void postProcess(ProjectDescriptor descriptor, File projectDescriptorFile) throws IOException {
        File projectRoot = projectDescriptorFile.getParentFile().getCanonicalFile();
        descriptor.setProjectFolder(projectRoot);
        processModulePathPatterns(descriptor, projectRoot);

        for (Module module : descriptor.getModules()) {
            module.setProject(descriptor);
            if (module.getMethodFilter() == null) {
                module.setMethodFilter(new MethodFilter());
            }
            if (module.getMethodFilter().getExcludes() == null) {
                module.getMethodFilter().setExcludes(new HashSet<String>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getExcludes().removeAll(Arrays.asList("", null));
            }

            if (module.getMethodFilter().getIncludes() == null) {
                module.getMethodFilter().setIncludes(new HashSet<String>());
            } else {
                // Remove empty nodes
                module.getMethodFilter().getIncludes().removeAll(Arrays.asList("", null));
            }

            if (!new File(module.getRulesRootPath().getPath()).isAbsolute()) {
                PathEntry absolutePath = new PathEntry(
                    new File(projectRoot, module.getRulesRootPath().getPath()).getCanonicalFile().getAbsolutePath());
                module.setRulesRootPath(absolutePath);
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
