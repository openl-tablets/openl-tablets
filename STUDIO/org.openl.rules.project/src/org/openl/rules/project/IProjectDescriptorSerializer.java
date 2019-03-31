package org.openl.rules.project;

import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;

public interface IProjectDescriptorSerializer {

    ProjectDescriptor deserialize(InputStream source);

    String serialize(ProjectDescriptor source);
}
