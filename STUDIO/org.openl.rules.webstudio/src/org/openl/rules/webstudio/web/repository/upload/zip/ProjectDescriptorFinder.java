package org.openl.rules.webstudio.web.repository.upload.zip;

import static org.openl.rules.util.Strings.format;

import java.io.InputStream;
import javax.xml.bind.JAXBException;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.webstudio.web.repository.upload.ProjectDescriptorUtils;

public class ProjectDescriptorFinder extends DefaultZipEntryCommand {
    private final XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private ProjectDescriptor projectDescriptor;
    private boolean exists;
    private JAXBException serializationException;


    @Override
    public boolean execute(String filePath, InputStream inputStream) {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(filePath)) {
            exists = true;
            try {
                projectDescriptor = serializer.deserialize(inputStream);
            } catch (JAXBException e) {
                serializationException = e;
            }
            return false;
        }
        return true;
    }

    public ProjectDescriptor getProjectDescriptor() throws ProjectDescriptionException {
        if (!exists) {
            throw new ProjectDescriptionNotFoundException(
                    format("Project descriptor file %s not found",
                            ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME));
        }
        if (serializationException != null) {
            throw new InvalidProjectDescriptorFileFormatException(ProjectDescriptorUtils.getErrorMessage(serializationException));
        }
        return projectDescriptor;
    }

    public boolean isExists() {
        return exists;
    }
}
