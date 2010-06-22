package org.openl.rules.project;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import java.io.OutputStream;
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

    public ProjectDescriptor readDescriptor(InputStream source) {
        return serializer.deserialize(source);
    }

    public ProjectDescriptor readDescriptor(String filename) throws FileNotFoundException, ValidationException {
        File source = new File(filename);
        FileInputStream inputStream = new FileInputStream(source);

        ProjectDescriptor descriptor = readDescriptor(inputStream);
        validator.validate(descriptor);

        return descriptor;
    }

    public void writeDescriptor(ProjectDescriptor descriptor, OutputStream dest) throws IOException,
                                                                                ValidationException {
        validator.validate(descriptor);

        String serializedObject = serializer.serialize(descriptor);
        dest.write(serializedObject.getBytes());
    }
}
