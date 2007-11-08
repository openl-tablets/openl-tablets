package org.openl.rules.webstudio.services.upload;

import org.apache.myfaces.custom.fileupload.UploadedFile;

import org.openl.rules.webstudio.services.ServiceParams;


/**
 * Parameters for {@link UploadService}.
 *
 * @author Andrey Naumenko
 */
public class UploadServiceParams extends ServiceParams {
    private static final long serialVersionUID = 1L;
    private UploadedFile file;

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
}
