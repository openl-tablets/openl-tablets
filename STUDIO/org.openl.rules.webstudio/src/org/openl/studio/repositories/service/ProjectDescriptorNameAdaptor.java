package org.openl.studio.repositories.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import jakarta.xml.bind.JAXBException;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.repository.folder.FileAdaptor;

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
        return ProjectDescriptor.FILE_NAME.equals(fileName);
    }

    @Override
    public InputStream apply(InputStream inputStream) throws IOException, JAXBException {
        // Read the stream to memory and try to parse it and then change project name. If it cannot be parsed return
        // original rules.xml.
        var originalBytes = inputStream.readAllBytes();
        try {
            var projectDescriptor = ProjectDescriptor.read(new ByteArrayInputStream(originalBytes));
            projectDescriptor.setName(projectName);
            return new ByteArrayInputStream(projectDescriptor.toBytes());
        } catch (Exception e) {
            return new ByteArrayInputStream(originalBytes);
        }
    }
}
