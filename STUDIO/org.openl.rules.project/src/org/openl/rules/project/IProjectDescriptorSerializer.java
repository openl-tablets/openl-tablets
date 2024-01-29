package org.openl.rules.project;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBException;

import org.openl.rules.project.model.ProjectDescriptor;

public interface IProjectDescriptorSerializer {

    ProjectDescriptor deserialize(InputStream source) throws JAXBException;

    String serialize(ProjectDescriptor source) throws IOException, JAXBException;
}
