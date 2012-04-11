package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ActionEvent;

import org.richfaces.model.UploadItem;

public class UploadExcelDiffController extends ExcelDiffController {

    /**
     * Then name of file which should be removed from list of files to compare.
     * NOTE: it is not used directly by controller but required for action
     * listener invocation using ajax request.
     */
    private String fileName;
    private List<UploadItem> uploadedFiles = new ArrayList<UploadItem>();

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<UploadItem> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadItem> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public int getUploadsAvailable() {
        return MAX_FILES_COUNT - uploadedFiles.size();
    }

    public void clearUploadData(ActionEvent event) {
        uploadedFiles.clear();
    }

    public String compare() {
        // fix Ctrl+R in browser
        if (uploadedFiles.size() >= MAX_FILES_COUNT) {
            
            List<File> filesToCompare = new ArrayList<File>();
            for (UploadItem uploadedFile : uploadedFiles) {
                filesToCompare.add(uploadedFile.getFile());
            }
            compare(filesToCompare);

            // Clean up
            for (UploadItem uploadedFile : uploadedFiles) {
                uploadedFile.getFile().delete();
            }
            clearUploadData(null);

        }

        return null;
    }

    public void compare(List<File> files) {
        setFilesToCompare(files);
        super.compare();
    }

}
