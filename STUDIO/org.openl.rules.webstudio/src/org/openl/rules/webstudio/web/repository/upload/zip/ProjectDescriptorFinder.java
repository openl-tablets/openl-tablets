package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

public class ProjectDescriptorFinder extends DefaultZipEntryCommand {
    private final XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
    private ProjectDescriptor projectDescriptor;

    @Override
    public boolean execute(String filePath, InputStream inputStream) {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(filePath)) {
            projectDescriptor = serializer.deserialize(inputStream);
            return false;
        }
        return true;
    }

    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }
}
