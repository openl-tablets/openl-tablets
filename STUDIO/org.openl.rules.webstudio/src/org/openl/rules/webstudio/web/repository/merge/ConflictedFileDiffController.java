package org.openl.rules.webstudio.web.repository.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.validation.ValidationException;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.MergeConflictException;
import org.openl.rules.webstudio.web.diff.ExcelDiffController;
import org.openl.rules.webstudio.web.util.Constants;
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

@ManagedBean
@SessionScoped
public class ConflictedFileDiffController extends ExcelDiffController {
    private final Logger log = LoggerFactory.getLogger(ConflictedFileDiffController.class);

    @ManagedProperty(value = "#{workspaceManager}")
    private MultiUserWorkspaceManager workspaceManager;
    private String conflictedFile;

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

            MergeConflictInfo mergeConflict = getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                WorkspaceUser user = new WorkspaceUserImpl(
                    SecurityContextHolder.getContext().getAuthentication().getName());
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

                FileItem their = userWorkspace.getDesignTimeRepository()
                    .getRepository()
                    .readHistory(conflictedFile, exception.getTheirCommit());
                File theirFile = createTempFile(their, conflictedFile);

                String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                String localName = conflictedFile.substring(rulesLocation.length());
                FileItem our = userWorkspace.getLocalWorkspace().getRepository().read(localName);
                File ourFile = createTempFile(our, localName);

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
        super.setShowEqualElements(false);
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    public String getDiff() {
        if (StringUtils.isBlank(conflictedFile) || isExcelFile(conflictedFile)) {
            return null;
        }

        MergeConflictInfo mergeConflict = getMergeConflict();
        if (mergeConflict != null) {
            MergeConflictException exception = mergeConflict.getException();
            return exception.getDiffs().get(conflictedFile);
        }

        return null;
    }

    private MergeConflictInfo getMergeConflict() {
        return (MergeConflictInfo) FacesUtils.getSessionMap().get(Constants.SESSION_PARAM_MERGE_CONFLICT);
    }

    private boolean isExcelFile(String conflictedFile) {
        return conflictedFile.endsWith(".xls") || conflictedFile.endsWith(".xlsx");
    }
}
