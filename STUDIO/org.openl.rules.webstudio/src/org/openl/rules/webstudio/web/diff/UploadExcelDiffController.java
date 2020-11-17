package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.richfaces.component.UITree;
import org.richfaces.event.FileUploadEvent;
import org.richfaces.model.UploadedFile;

@ManagedBean
@SessionScoped
public class UploadExcelDiffController extends ExcelDiffController {

    private List<UploadedFile> uploadedFiles = new ArrayList<>();

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

    @Override
    public String compare() {
        // Fix Ctrl+R in browser
        if (uploadedFiles.size() >= MAX_FILES_COUNT) {
            // Clear selection to handle NPE bug. See EPBDS-3992 for details.
            UITree treeComponent = (UITree) FacesContext.getCurrentInstance()
                .getViewRoot()
                .findComponent("diffTreeForm:newTree");
            treeComponent.setSelection(new ArrayList<>());

            deleteTempFiles();
            List<File> filesToCompare = new ArrayList<>();
            for (UploadedFile uploadedFile : uploadedFiles) {
                File fileToCompare = FileTool.toTempFile(uploadedFile.getInputStream(),
                    FileUtils.getName(uploadedFile.getName()));
                filesToCompare.add(fileToCompare);
                // Files can be reloaded lazily later. We cannot delete them immediately. Instead delete them when Bean
                // is destroyed (on session timeout) or before next comparison.
                addTempFile(fileToCompare);
            }
            compare(filesToCompare);

            // Clear uploaded files
            uploadedFiles.clear();
        }

        return null;
    }

    @Override
    public void compare(List<File> files) {
        setFilesToCompare(files);
        super.compare();
    }

    @PreDestroy
    public void destroy() {
        deleteTempFiles();
    }
}
