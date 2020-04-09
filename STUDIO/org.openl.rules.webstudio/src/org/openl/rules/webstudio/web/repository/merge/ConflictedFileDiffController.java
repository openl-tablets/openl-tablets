package org.openl.rules.webstudio.web.repository.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.PreDestroy;
import javax.faces.component.UIComponent;
import javax.validation.ValidationException;

import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.webstudio.web.diff.ExcelDiffController;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

@Controller
@SessionScope
public class ConflictedFileDiffController extends ExcelDiffController {
    private final Logger log = LoggerFactory.getLogger(ConflictedFileDiffController.class);

    private final MultiUserWorkspaceManager workspaceManager;
    private String conflictedFile;

    public ConflictedFileDiffController(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public void setConflictedFile(String conflictedFile) {
        try {
            deleteTempFiles();
            setDiffTree(null);
            clearTreeSelection();

            this.conflictedFile = conflictedFile;

            if (StringUtils.isBlank(conflictedFile)) {
                return;
            }

            if (!isExcelFile(conflictedFile)) {
                return;
            }

            MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                WorkspaceUser user = new WorkspaceUserImpl(
                    SecurityContextHolder.getContext().getAuthentication().getName());
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

                FileItem their = userWorkspace.getDesignTimeRepository()
                    .getRepository()
                    .readHistory(conflictedFile, exception.getTheirCommit());
                File theirFile = createTempFile(their, conflictedFile);
                File ourFile;

                FileItem our;
                if (mergeConflict.isMerging()) {
                    our = userWorkspace.getDesignTimeRepository()
                        .getRepository()
                        .readHistory(conflictedFile, exception.getYourCommit());
                    ourFile = createTempFile(our, conflictedFile);
                    if (mergeConflict.isExportOperation()) {
                        File tmp = ourFile;
                        ourFile = theirFile;
                        theirFile = tmp;
                    }
                } else {
                    String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                    String localName = conflictedFile.substring(rulesLocation.length());
                    our = userWorkspace.getLocalWorkspace().getRepository().read(localName);
                    ourFile = createTempFile(our, conflictedFile);
                }

                compare(Arrays.asList(theirFile, ourFile));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(e.getMessage(), e);
        }
    }

    @Override
    public void setShowEqualElements(boolean showEqualElements) {
        super.setShowEqualElements(showEqualElements);
        clearTreeSelection();
        setDiffTree(getRichDiffTree().getDiffTreeNode());
    }

    private void clearTreeSelection() {
        UIComponent treeComponent = RichFunction.findComponent("newTree");
        if (treeComponent instanceof UITree) {
            ((UITree) treeComponent).setSelection(Collections.emptyList());
        }
    }

    private File createTempFile(FileItem item, String fullName) throws FileNotFoundException {
        if (item == null) {
            return null;
        }
        File ourFile = FileTool.toTempFile(item.getStream(), FileUtils.getName(fullName));
        if (ourFile == null) {
            throw new FileNotFoundException(String.format("Cannot create temp file for '%s'", fullName));
        }
        addTempFile(ourFile);
        return ourFile;
    }

    @PreDestroy
    public void destroy() {
        deleteTempFiles();
        setDiffTree(null);
    }

    public void close() {
        setFilesToCompare(Collections.emptyList());
        deleteTempFiles();
        setDiffTree(null);
        clearTreeSelection();
        super.setShowEqualElements(false);
    }

    public String getDiff() {
        if (StringUtils.isBlank(conflictedFile) || isExcelFile(conflictedFile)) {
            return null;
        }

        MergeConflictInfo mergeConflict = ConflictUtils.getMergeConflict();
        if (mergeConflict != null) {
            MergeConflictException exception = mergeConflict.getException();
            return exception.getDiffs().get(conflictedFile);
        }

        return null;
    }

    private boolean isExcelFile(String conflictedFile) {
        return conflictedFile.endsWith(".xls") || conflictedFile.endsWith(".xlsx");
    }
}
