package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.io.FilenameUtils;
import org.openl.util.FileTool;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

@ManagedBean
@SessionScoped
public class UploadExcelDiffController extends ExcelDiffController {

    private List<File> uploadedFiles = new ArrayList<File>();

    public int getUploadsSize() {
        return uploadedFiles.size();
    }

    public void uploadListener(FileUploadEvent event) throws IOException {
        UploadedFile file = event.getUploadedFile();
        File uploadedFile = FileTool.toTempFile(
                file.getInputStream(), FilenameUtils.getName(file.getName()));
        uploadedFiles.add(uploadedFile);
    }

    public String compare() {
        // Fix Ctrl+R in browser
        if (uploadedFiles.size() >= MAX_FILES_COUNT) {

            compare(uploadedFiles);

            // Clear uploaded files
            for (File file : uploadedFiles) {
                file.delete();
            }
            uploadedFiles.clear();
        }

        return null;
    }

    public void compare(List<File> files) {
        setFilesToCompare(files);
        super.compare();
    }

}
