package org.openl.rules.webstudio.web.repository.upload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import com.thoughtworks.xstream.XStreamException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.project.xml.XmlProjectDescriptorSerializer;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.xml.sax.SAXParseException;

public abstract class AProjectCreator {

    private final Log log = LogFactory.getLog(AProjectCreator.class);

    private String projectName;
    private UserWorkspace userWorkspace;

    public AProjectCreator(String projectName, UserWorkspace userWorkspace) {
        this.projectName = projectName;
        this.userWorkspace = userWorkspace;
    }

    protected String getProjectName() {
        return projectName;
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

            if (projectBuilder.getProject().getArtefacts().size() == 0) {
                projectBuilder.getProject().delete();
                FacesUtils.addErrorMessage("");
            }

            projectBuilder.getProject().edit();
        } catch (Exception e) {
            if (projectBuilder != null) {
                projectBuilder.cancel();
            }

            log.error("Error creating project.", e);
            errorMessage = e.getMessage();

            // add detailed information
            Throwable cause = e.getCause();

            if (cause != null) {
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }

                if (cause.getMessage() != null) {
                    errorMessage += " Cause: " + cause.getMessage();
                }
            }
        }
        return errorMessage;
    }

    protected InputStream changeFileIfNeeded(String fileName, InputStream inputStream) throws UnsupportedEncodingException, ProjectException {
        if (ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME.equals(fileName)) {
            try {
                XmlProjectDescriptorSerializer serializer = new XmlProjectDescriptorSerializer(false);
                ProjectDescriptor projectDescriptor = serializer.deserialize(inputStream);
                projectDescriptor.setName(getProjectName());
                inputStream = new ByteArrayInputStream(serializer.serialize(projectDescriptor).getBytes("UTF-8"));
            } catch (XStreamException e) {
                StringBuilder message = new StringBuilder("Can't parse rules.xml.");
                if (e.getCause() instanceof SAXParseException) {
                    SAXParseException parseException = (SAXParseException) e.getCause();
                    message.append(" Line number: ").append(parseException.getLineNumber())
                            .append(", column number: ").append(parseException.getColumnNumber())
                            .append(".");
                }
                throw new ProjectException(message.toString(), e);
            } finally {
                IOUtils.closeQuietly(inputStream);
            }
        }
        return inputStream;
    }

    protected abstract RulesProjectBuilder getProjectBuilder() throws ProjectException;

    public abstract void destroy();

}
