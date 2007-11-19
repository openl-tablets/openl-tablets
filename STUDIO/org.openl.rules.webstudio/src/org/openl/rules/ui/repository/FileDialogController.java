package org.openl.rules.ui.repository;

public class FileDialogController{
    private String uploadFrom;
    private String fileName;

    /**
     * Adds new file to active node (Project or Folder)
     *
     * @return outcome: "success" or "fail"
     */
    public String addFile() {
        return null;
    }

    public String getFileName() {
        return null;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getUploadFrom() {
        // TODO: later return null -- browser's security won't allow it anyway
        return uploadFrom;
    }

    public void setUploadFrom(String uploadFrom) {
        this.uploadFrom = uploadFrom;
    }

    /**
     * Updates file (active node)
     *
     * @return
     */
    public String updateFile() {
        return null;
    }
}
