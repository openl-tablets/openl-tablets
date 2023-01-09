package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

public class ProjectDescriptorFinder extends DefaultZipEntryCommand {
    private final XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private ProjectDescriptor projectDescriptor;
    private boolean projectDescriptorReadFailed;


    @Override
    public boolean execute(String filePath, InputStream inputStream) {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(filePath)) {
            try {
                projectDescriptor = serializer.deserialize(inputStream);
            } catch (JAXBException e) {
                projectDescriptorReadFailed = true;
                final Logger log = LoggerFactory.getLogger(ProjectDescriptorFinder.class);
                log.error("Error during Project Description deserialization", e);
            }
            return false;
        }
        return true;
    }

    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }

    public boolean isProjectDescriptorReadFailed() {
        return projectDescriptorReadFailed;
    }
}
