package org.openl.rules.webstudio.services.upload;

import java.io.File;

import org.openl.rules.webstudio.services.ServiceResult;


/**
 * Result of execution of {@link UploadService}.
 *
 * @author Andrey Naumenko
 */
public class UploadServiceResult extends ServiceResult {
    private File resultFile;
    private int uploadCount;
    private File[] resultFiles;

    /**
     * Uploaded file. For {@link UploadService} contains uploaded files. For
     * {@link UploadCatalogFilesService} valid only if user upload non-zip file or zip
     * file contains one catalog file.
     *
     * @return file name.
     */
    public File getResultFile() {
        return resultFile;
    }

    public void setResultFile(File resultFile) {
        this.resultFile = resultFile;
    }

    /**
     * Uploaded files. Contains uploaded files if more then one.
     *
     * @return uploaded files
     */
    public File[] getResultFiles() {
        return resultFiles;
    }

    public void setResultFiles(File[] resultFiles) {
        this.resultFiles = resultFiles;
    }

    /**
     * Number of uploaded files (can be different from 1 if user has uploaded zip
     * file with more than 1 file inside).<p>This values is always <code>1</code>
     * for {@link UploadService}, but can have other values for {@link
     * UploadCatalogFilesService}.</p>
     *
     * @return number of uploaded files.
     */
    public int getUploadCount() {
        return uploadCount;
    }

    public void setUploadCount(int uploadCount) {
        this.uploadCount = uploadCount;
    }
}
