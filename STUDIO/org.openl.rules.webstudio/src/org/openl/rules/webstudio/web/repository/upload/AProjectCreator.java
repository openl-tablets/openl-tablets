package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AProjectCreator {

    private final Logger log = LoggerFactory.getLogger(AProjectCreator.class);

    private String projectName;
    private String projectFolder;
    private UserWorkspace userWorkspace;

    public AProjectCreator(String projectName, String projectFolder, UserWorkspace userWorkspace) {
        this.projectName = projectName;
        this.projectFolder = projectFolder;
        this.userWorkspace = userWorkspace;
    }

    protected String getProjectName() {
        return projectName;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    protected UserWorkspace getUserWorkspace() {
        return userWorkspace;
    }

    /**
     * @return error message that had occured during the project creation. In other case null.
     */
    public String createRulesProject() {
        String errorMessage = null;
        RulesProjectBuilder projectBuilder = null;
        try {
            projectBuilder = getProjectBuilder();
            projectBuilder.save();
            projectBuilder.getProject().open();
        } catch (Exception e) {
            log.error("Error creating project.", e);

            if (projectBuilder != null) {
                projectBuilder.cancel();
            }

            errorMessage = e.getMessage();

            // add detailed information
            Throwable cause = e.getCause();

            if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                if (cause.getMessage() != null && !cause.getMessage().equals(errorMessage)) {
                    errorMessage += " Cause: " + cause.getMessage();
                }
            }

            if (errorMessage == null) {
                errorMessage = "Error creating project";
            }
        }
        return errorMessage;
    }

    protected InputStream changeFileIfNeeded(String fileName, InputStream inputStream) throws ProjectException {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(fileName)) {
            // Read the stream to memory and try to parse it and then change project name. If it cannot be parsed return
            // original rules.xml.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                IOUtils.copyAndClose(inputStream, outputStream);
            } catch (IOException e) {
                throw new ProjectException(e.getMessage(), e);
            }
            ByteArrayInputStream copy = new ByteArrayInputStream(outputStream.toByteArray());

            try {
                XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer(false);
                ProjectDescriptor projectDescriptor = serializer.deserialize(copy);
                projectDescriptor.setName(getProjectName());
                return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
                copy.reset();
                return copy;
            }
        } else {
            return inputStream;
        }
    }

    protected abstract RulesProjectBuilder getProjectBuilder() throws ProjectException;

    public abstract void destroy();

}
