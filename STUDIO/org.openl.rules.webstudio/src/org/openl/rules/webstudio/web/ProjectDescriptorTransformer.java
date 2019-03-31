package org.openl.rules.webstudio.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.IProjectDescriptorSerializer;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.ResourceTransformer;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.ProjectDescriptorSerializerFactory;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectDescriptorTransformer implements ResourceTransformer {
    private final Logger log = LoggerFactory.getLogger(ProjectDescriptorTransformer.class);
    private final String newProjectName;

    public ProjectDescriptorTransformer(String newProjectName) {
        this.newProjectName = newProjectName;
    }

    @Override
    public InputStream transform(AProjectResource resource) throws ProjectException {
        if (isProjectDescriptor(resource)) {
            // Read the stream to memory and try to parse it and then change project name. If it can't be parsed return
            // original rules.xml.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                IOUtils.copyAndClose(resource.getContent(), outputStream);
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            }
            ByteArrayInputStream copy = new ByteArrayInputStream(outputStream.toByteArray());

            try {
                IProjectDescriptorSerializer serializer = WebStudioUtils
                    .getBean(ProjectDescriptorSerializerFactory.class)
                    .getSerializer(resource);
                ProjectDescriptor projectDescriptor = serializer.deserialize(copy);
                projectDescriptor.setName(newProjectName);
                return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                copy.reset();
                return copy;
            }
        }

        return resource.getContent();
    }

    private boolean isProjectDescriptor(AProjectResource resource) {
        return ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(resource.getInternalPath());
    }
}
