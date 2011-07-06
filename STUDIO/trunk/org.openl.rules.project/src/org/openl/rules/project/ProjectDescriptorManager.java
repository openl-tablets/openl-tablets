package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;

import org.openl.rules.project.model.Module;
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
    
    private void postProcess(ProjectDescriptor descriptor, File projectDescriptorFile) {
        
        File projectRoot = projectDescriptorFile.getParentFile();
        descriptor.setProjectFolder(projectRoot);
        
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
