package org.openl.rules.project;

import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;

import javax.xml.bind.JAXBException;

public interface IProjectDescriptorSerializer {

    ProjectDescriptor deserialize(InputStream source) throws JAXBException;

    String serialize(ProjectDescriptor source) throws IOException, JAXBException;
}
