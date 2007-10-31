package org.openl.rules.ui.repository;

import org.openl.rules.ui.repository.beans.FileBean;
import org.openl.rules.ui.repository.beans.FolderBean;
import org.openl.rules.ui.repository.handlers.FileHandler;
import org.openl.rules.ui.repository.handlers.FolderHandler;

public class FileDialogController extends AbstractDialogController {
    private String uploadFrom;
    private String fileName;
    
    /**
     * Adds new file to active node (Project or Folder)
     * 
     * @return outcome: "success" or "fail"
     */
    public String addFile() {
        FolderHandler fh = getContext().getFolderHandler();
        FolderBean bean = (FolderBean) getContext().getActiveNodeBean();

        boolean success = fh.addFile(bean, fileName, uploadFrom);
        refresh();
        return outcome(success);
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
        FileHandler fh = getContext().getFileHandler();
        FileBean bean = (FileBean) getContext().getActiveNodeBean();
        
        return outcome(fh.updateFile(bean, uploadFrom));
    }
}
