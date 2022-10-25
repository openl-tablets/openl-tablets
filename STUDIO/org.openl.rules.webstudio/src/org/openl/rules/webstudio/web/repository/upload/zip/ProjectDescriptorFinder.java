package org.openl.rules.webstudio.web.repository.upload.zip;

import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;

import javax.xml.bind.JAXBException;

public class ProjectDescriptorFinder extends DefaultZipEntryCommand {
    private XmlProjectDescriptorSerializer serializer;
    private ProjectDescriptor projectDescriptor;

    @Override
    public boolean execute(String filePath, InputStream inputStream) throws JAXBException {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(filePath)) {
            projectDescriptor = getSerializer().deserialize(inputStream);
            return false;
        }
        return true;
    }

    private XmlProjectDescriptorSerializer getSerializer() throws JAXBException {
        if (serializer == null) {
            serializer = new XmlProjectDescriptorSerializer();
        }
        return serializer;
    }

    public ProjectDescriptor getProjectDescriptor() {
        return projectDescriptor;
    }
}
