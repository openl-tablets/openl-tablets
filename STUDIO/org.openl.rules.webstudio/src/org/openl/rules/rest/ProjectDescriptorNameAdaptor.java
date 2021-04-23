package org.openl.rules.rest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.folder.FileAdaptor;
import org.openl.util.IOUtils;

public class ProjectDescriptorNameAdaptor implements FileAdaptor {

    private final String projectName;

    public ProjectDescriptorNameAdaptor(String projectName) {
        this.projectName = Objects.requireNonNull(projectName);
    }

    @Override
    public boolean accept(Path path) {
        String fileName = path.toString();
        if (fileName.charAt(0) == '/' || fileName.charAt(0) == '\\') {
            fileName = fileName.substring(1);
        }
        return ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(fileName);
    }

    @Override
    public InputStream apply(InputStream inputStream) throws IOException {
        // Read the stream to memory and try to parse it and then change project name. If it cannot be parsed return
        // original rules.xml.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        IOUtils.copyAndClose(inputStream, outputStream);
        ByteArrayInputStream copy = new ByteArrayInputStream(outputStream.toByteArray());
        XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
        ProjectDescriptor projectDescriptor = serializer.deserialize(copy);
        projectDescriptor.setName(projectName);
        return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
    }

}
