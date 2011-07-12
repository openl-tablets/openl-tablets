package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.model.validation.ProjectDescriptorValidator;
import org.openl.rules.project.model.validation.ValidationException;

import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

public class ProjectDescriptorManager {

    private IProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private ProjectDescriptorValidator validator = new ProjectDescriptorValidator();

    public IProjectDescriptorSerializer getSerializer() {
        return serializer;
    }

    public void setSerializer(IProjectDescriptorSerializer serializer) {
        this.serializer = serializer;
    }

    private ProjectDescriptor readDescriptorInternal(InputStream source) {
        return serializer.deserialize(source);
    }

    public ProjectDescriptor readDescriptor(File filename) throws FileNotFoundException, ValidationException {
        FileInputStream inputStream = new FileInputStream(filename);

        ProjectDescriptor descriptor = readDescriptorInternal(inputStream);
        postProcess(descriptor, filename);
        validator.validate(descriptor);

        return descriptor;
    }
    
    public ProjectDescriptor readDescriptor(String filename) throws FileNotFoundException, ValidationException {
        File source = new File(filename);
        return readDescriptor(source);
    }

    public void writeDescriptor(ProjectDescriptor descriptor, OutputStream dest) throws IOException,
                                                                                ValidationException {
        validator.validate(descriptor);

        String serializedObject = serializer.serialize(descriptor);
        dest.write(serializedObject.getBytes());
    }
    
    private boolean isModuleWithPathPattern(Module module) {
        if (module.getRulesRootPath() != null) {
            return module.getRulesRootPath().getPath().contains("*") || module.getRulesRootPath().getPath().contains("?");
        }
        return false;
    }
    
    private void check(File folder, List<File> matched, Pattern pathPattern, File rootFolder) {
        File[] files = folder.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                check(file, matched, pathPattern, rootFolder);
            } else {
                String relativePath = file.getAbsolutePath().substring((int) rootFolder.getAbsolutePath().length()+1);
                relativePath = relativePath.replace("\\", "/");
                if (pathPattern.matcher(relativePath).matches()) {
                    matched.add(file);
                }
            }
        }
    }

    private List<Module> getAllModulesMatchingPathPattern(ProjectDescriptor descriptor, String pathPattern) {
        List<Module> modules = new ArrayList<Module>();
        String pattern = pathPattern.replace("**", ".*");
        pattern = pattern.replace("?", ".");
        pattern = pattern.replace("\\", "/");
        pattern = pattern.replace(".", "\\.");
        pattern = pattern.replace("*", "[^/]*");
        
        List<File> files = new ArrayList<File>();
        check(descriptor.getProjectFolder(), files, Pattern.compile(pattern), descriptor.getProjectFolder());

        for (File file : files) {
            Module module = new Module();
            module.setProject(descriptor);
            module.setRulesRootPath(new PathEntry(file.getAbsolutePath()));
            module.setName(file.getName());
            module.setType(ModuleType.API);
            modules.add(module);
        }
        return modules;
    }

    private void processModulePathPatterns(ProjectDescriptor descriptor) {
        List<Module> modulesWasRead = descriptor.getModules();
        List<Module> processedModules = new ArrayList<Module>(modulesWasRead.size());
        for (Module module : modulesWasRead) {
            if (isModuleWithPathPattern(module)) {
                processedModules.addAll(getAllModulesMatchingPathPattern(descriptor, module.getRulesRootPath()
                        .getPath()));
            } else {
                processedModules.add(module);
            }
        }
        descriptor.setModules(processedModules);
    }

    private void postProcess(ProjectDescriptor descriptor, File projectDescriptorFile) {
        
        File projectRoot = projectDescriptorFile.getParentFile();
        descriptor.setProjectFolder(projectRoot);
        processModulePathPatterns(descriptor);

        for (Module module : descriptor.getModules()) {
            module.setProject(descriptor);
            if (!new File(module.getRulesRootPath().getPath()).isAbsolute()) {
                PathEntry absolutePath = new PathEntry(
                        new File(projectRoot, module.getRulesRootPath().getPath()).getAbsolutePath());
                module.setRulesRootPath(absolutePath);
            }
        }
    }

}
