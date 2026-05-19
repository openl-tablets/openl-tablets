package org.openl.rules.webstudio.web.repository.upload.zip;

import static org.openl.rules.util.Strings.format;

import java.io.InputStream;

import org.openl.rules.project.model.ProjectDescriptor;

public class ProjectDescriptorFinder extends DefaultZipEntryCommand {
    private ProjectDescriptor projectDescriptor;
    private boolean exists;


    @Override
    public boolean execute(String filePath, InputStream inputStream) {
        if (ProjectDescriptor.FILE_NAME.equals(filePath)) {
            exists = true;
            projectDescriptor = ProjectDescriptor.read(inputStream);
            return false;
        }
        return true;
    }

    public ProjectDescriptor getProjectDescriptor() throws ProjectDescriptionException {
        if (!exists) {
            throw new ProjectDescriptionNotFoundException(
                    format("Project descriptor file %s not found",
                            ProjectDescriptor.FILE_NAME));
        }
        if (projectDescriptor == null) {
            throw new InvalidProjectDescriptorFileFormatException(
                    "Cannot parse project descriptor file " + ProjectDescriptor.FILE_NAME + '.');
        }
        return projectDescriptor;
    }

    public boolean isExists() {
        return exists;
    }
}
