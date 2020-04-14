package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.faces.context.FacesContext;

import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileTool;
import org.richfaces.component.UITree;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

@Controller
@SessionScope
public class UploadExcelDiffController extends ExcelDiffController {
    private final Logger log = LoggerFactory.getLogger(UploadExcelDiffController.class);

    private final List<ProjectFile> uploadedFiles = new ArrayList<>();

    public int getUploadsSize() {
        return uploadedFiles.size();
    }

    public void uploadListener(FileUploadEvent event) {
        try {
            uploadedFiles.add(new ProjectFile(event.getUploadedFile()));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public String compare() {
        try {
            // Fix Ctrl+R in browser
            if (uploadedFiles.size() >= MAX_FILES_COUNT) {
                // Clear selection to handle NPE bug. See EPBDS-3992 for details.
                UITree treeComponent = (UITree) FacesContext.getCurrentInstance()
                    .getViewRoot()
                    .findComponent("diffTreeForm:newTree");
                treeComponent.setSelection(new ArrayList<>());

                deleteTempFiles();
                List<File> filesToCompare = new ArrayList<>();
                for (ProjectFile uploadedFile : uploadedFiles) {
                    File fileToCompare = FileTool.toTempFile(uploadedFile.getInput(), uploadedFile.getName());
                    filesToCompare.add(fileToCompare);
                    // Files can be reloaded lazily later. We cannot delete them immediately. Instead delete them when Bean
                    // is destroyed (on session timeout) or before next comparison.
                    addTempFile(fileToCompare);
                }
                compare(filesToCompare);

                // Clear uploaded files
                clearUploadedFiles();
            }
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
            WebStudioUtils.addErrorMessage(e.getMessage());
            deleteTempFiles();
            clearUploadedFiles();
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
        clearUploadedFiles();
    }

    private void clearUploadedFiles() {
        for (ProjectFile uploadedFile : uploadedFiles) {
            uploadedFile.destroy();
        }
        uploadedFiles.clear();
    }
}
