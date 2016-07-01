package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

@ManagedBean
@SessionScoped
public class UploadExcelDiffController extends ExcelDiffController {

    private List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();

    public List<UploadedFile> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<UploadedFile> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public int getUploadsSize() {
        return uploadedFiles.size();
    }

    public void uploadListener(FileUploadEvent event) {
        UploadedFile file = event.getUploadedFile();
        uploadedFiles.add(file);
    }

    public String compare() {
        // Fix Ctrl+R in browser
        if (uploadedFiles.size() >= MAX_FILES_COUNT) {

            List<File> filesToCompare = new ArrayList<File>();
            for (UploadedFile uploadedFile : uploadedFiles) {
                File fileToCompare = FileTool.toTempFile(
                        uploadedFile.getInputStream(), FileUtils.getName(uploadedFile.getName()));
                filesToCompare.add(fileToCompare);
            }
            compare(filesToCompare);

            // Clear uploaded files
            uploadedFiles.clear();
            for (File file : filesToCompare) {
                file.delete();
            }
        }

        return null;
    }

    public void compare(List<File> files) {
        setFilesToCompare(files);
        super.compare();
    }

}
