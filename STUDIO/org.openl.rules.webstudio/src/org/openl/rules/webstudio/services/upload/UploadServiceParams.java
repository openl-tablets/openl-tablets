package org.openl.rules.webstudio.services.upload;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.webstudio.services.ServiceParams;
import org.openl.rules.workspace.uw.UserWorkspace;


/**
 * Parameters for {@link UploadService}.
 *
 * @author Andrey Naumenko
 */
public class UploadServiceParams extends ServiceParams {
    private static final long serialVersionUID = 1L;
    private UploadedFile file;
    private String projectName;
    private UserWorkspace workspace;

    /**
     * File to upload.
     *
     * @return file to upload.
     */
    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public UserWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(UserWorkspace workspace) {
        this.workspace = workspace;
    }
}
