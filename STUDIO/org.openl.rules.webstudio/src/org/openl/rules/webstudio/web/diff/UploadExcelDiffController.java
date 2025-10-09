package org.openl.rules.webstudio.web.diff;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import jakarta.annotation.PreDestroy;
import jakarta.faces.context.FacesContext;

import org.richfaces.component.UITree;
import org.richfaces.event.FileUploadEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.rest.ProjectHistoryService;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.webstudio.web.repository.project.ProjectFile;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.FileTool;

@Service
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

    /**
     * Remove uploaded files.
     *
     * @param fileNames file names split by '\n' symbol. If empty, all files will be removed.
     */
    public void setFileNamesToRemove(String fileNames) {
        if (fileNames.isEmpty()) {
            clearUploadedFiles();
        } else {
            List<String> toRemove = Arrays.asList(fileNames.split("\n"));
            for (Iterator<ProjectFile> iterator = uploadedFiles.iterator(); iterator.hasNext(); ) {
                ProjectFile file = iterator.next();
                if (toRemove.contains(file.getName())) {
                    file.destroy();
                    iterator.remove();
                }
            }
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
                    // Files can be reloaded lazily later. We cannot delete them immediately. Instead delete them when
                    // Bean
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

    public String compareVersions(String version1, String version2) {
        try {
            ProjectModel model = WebStudioUtils.getProjectModel();

            String historyStoragePath = model.getHistoryStoragePath();
            File file1ToCompare = ProjectHistoryService.get(historyStoragePath, version1);
            File file2ToCompare = ProjectHistoryService.get(historyStoragePath, version2);

            UploadExcelDiffController diffController = (UploadExcelDiffController) WebStudioUtils
                    .getBackingBean("uploadExcelDiffController");
            diffController.compare(Arrays.asList(file1ToCompare, file2ToCompare));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new Message("Error when comparing projects");
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
