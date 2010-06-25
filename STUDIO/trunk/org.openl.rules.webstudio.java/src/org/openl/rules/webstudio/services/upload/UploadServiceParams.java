package org.openl.rules.webstudio.services.upload;

import org.openl.rules.webstudio.services.ServiceParams;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.richfaces.model.UploadItem;

/**
 * Parameters for {@link UploadService}.
 *
 * @author Andrey Naumenko
 */
public class UploadServiceParams extends ServiceParams {
    private static final long serialVersionUID = 1L;
    private UploadItem file;
    private String projectName;
    private UserWorkspace workspace;
    private boolean unpackZipFile = true;

    /**
     * File to upload.
     *
     * @return file to upload.
     */
    public UploadItem getFile() {
        return file;
    }

    public String getProjectName() {
        return projectName;
    }

    public UserWorkspace getWorkspace() {
        return workspace;
    }

    /**
     * Flag attribute for upload service
     *
     * @return <code>true</code> then service will unpack uploaded zip files
     *         <code>false</code> then service will not unpack uploaded zip
     *         files
     */
    public boolean isUnpackZipFile() {
        return unpackZipFile;
    }

    public void setFile(UploadItem file) {
        this.file = file;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setUnpackZipFile(boolean unpackZipFile) {
        this.unpackZipFile = unpackZipFile;
    }

    public void setWorkspace(UserWorkspace workspace) {
        this.workspace = workspace;
    }
}
