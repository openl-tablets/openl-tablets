package org.openl.rules.openapi;

import java.io.IOException;

import org.openl.rules.model.scaffolding.ProjectModel;

public interface OpenAPIModelConverter {

    ProjectModel extractProjectModel(String pathTo) throws IOException;

}
