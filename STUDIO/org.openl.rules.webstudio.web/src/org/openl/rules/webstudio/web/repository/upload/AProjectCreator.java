package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.repository.git.MergeConflictException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;

public abstract class AProjectCreator {

    private static final Logger LOG = LoggerFactory.getLogger(AProjectCreator.class);

    private final String projectName;
    private final String projectFolder;
    private final UserWorkspace userWorkspace;
    private String createdProjectName;
    private Map<String, String> tags;

    public AProjectCreator(String projectName, String projectFolder, UserWorkspace userWorkspace, Map<String, String> tags) {
        this.projectName = projectName;
        this.projectFolder = projectFolder;
        this.userWorkspace = userWorkspace;
        this.tags = tags;
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
     * @return created project
     * @throws ProjectException if creating a project is failed
     */
    public RulesProject createRulesProject() throws ProjectException {
        RulesProjectBuilder projectBuilder = null;
        try {
            projectBuilder = getProjectBuilder();
            projectBuilder.getProject().saveTags(tags);
            projectBuilder.save();
            createdProjectName = projectBuilder.getCreateProjectName();
            return projectBuilder.getProject();
        } catch (Exception e) {
            Throwable cause = e.getCause();
            if (projectBuilder != null && cause instanceof MergeConflictException) {
                LOG.debug("Failed to save the project because of merge conflict.", cause);
                // Try to save second time. It should resolve the issue if conflict in openl-projects.properties file.
                try {
                    projectBuilder.save();
                    createdProjectName = projectBuilder.getCreateProjectName();
                    return projectBuilder.getProject();
                } catch (Exception ex) {
                    projectBuilder.cancel();
                    throw new ProjectException("Project creating is failed.", ex);
                }
            } else {
                if (projectBuilder != null) {
                    projectBuilder.cancel();
                }
                throw new ProjectException("Project creating is failed.", e);
            }
        }
    }

    public String getCreatedProjectName() {
        return createdProjectName;
    }

    private String getErrorMessage(Exception e) {
        String errorMessage = e.getMessage();

        Throwable cause = e.getCause();
        // add detailed information
        if (cause != null) {
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }

            if (cause.getMessage() != null && !cause.getMessage().equals(errorMessage)) {
                errorMessage += " Cause: " + cause.getMessage();
            }
        }

        if (errorMessage == null) {
            if (cause instanceof MergeConflictException) {
                errorMessage = "Merge conflict when create a project.";
            } else {
                errorMessage = "Error creating project.";
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
                XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer();
                ProjectDescriptor projectDescriptor = serializer.deserialize(copy);
                projectDescriptor.setName(getProjectName());
                return IOUtils.toInputStream(serializer.serialize(projectDescriptor));
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
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
