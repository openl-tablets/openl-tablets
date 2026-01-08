package org.openl.rules.webstudio.web.repository.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import jakarta.annotation.PreDestroy;
import jakarta.faces.component.UIComponent;

import org.richfaces.component.UITree;
import org.richfaces.function.RichFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.ui.Message;
import org.openl.rules.webstudio.service.UserManagementService;
import org.openl.rules.webstudio.web.diff.ExcelDiffController;
import org.openl.rules.workspace.MultiUserWorkspaceManager;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.WorkspaceUserImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.studio.projects.model.ProjectIdModel;
import org.openl.studio.projects.model.merge.MergeConflictInfo;
import org.openl.studio.projects.service.merge.ProjectsMergeConflictsSessionHolder;
import org.openl.util.FileTool;
import org.openl.util.FileUtils;
import org.openl.util.StringUtils;

@Service
@SessionScope
public class ConflictedFileDiffController extends ExcelDiffController {
    private static final Logger LOG = LoggerFactory.getLogger(ConflictedFileDiffController.class);

    private final MultiUserWorkspaceManager workspaceManager;
    private String conflictedFile;
    private final UserManagementService userManagementService;
    private final ProjectsMergeConflictsSessionHolder conflictsSessionHolder;
    private ProjectIdModel projectId;

    public ConflictedFileDiffController(MultiUserWorkspaceManager workspaceManager,
                                        UserManagementService userManagementService, ProjectsMergeConflictsSessionHolder conflictsSessionHolder) {
        this.workspaceManager = workspaceManager;
        this.userManagementService = userManagementService;
        this.conflictsSessionHolder = conflictsSessionHolder;
    }

    public void setProjectId(String projectId) {
        this.projectId = ProjectIdModel.decode(projectId);
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

            // Try new projectId-based storage first, fall back to legacy ConflictUtils for backward compatibility
            MergeConflictInfo mergeConflict = projectId != null
                    ? conflictsSessionHolder.getConflictInfo(projectId)
                    : ConflictUtils.getMergeConflict();
            if (mergeConflict != null) {
                var conflictDetails = mergeConflict.details();
                String userName = SecurityContextHolder.getContext().getAuthentication().getName();
                WorkspaceUser user = new WorkspaceUserImpl(userName,
                        (username) -> Optional.ofNullable(userManagementService.getUser(username))
                                .map(usr -> new UserInfo(usr.getUsername(), usr.getEmail(), usr.getDisplayName()))
                                .orElse(null));

                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

                String repositoryId = mergeConflict.getRepositoryId();
                FileItem their = userWorkspace.getDesignTimeRepository()
                        .getRepository(repositoryId)
                        .readHistory(conflictedFile, conflictDetails.theirCommit());
                File theirFile = createTempFile(their, conflictedFile);
                File ourFile;

                FileItem our;
                if (mergeConflict.isMerging()) {
                    our = userWorkspace.getDesignTimeRepository()
                            .getRepository(repositoryId)
                            .readHistory(conflictedFile, conflictDetails.yourCommit());
                    ourFile = createTempFile(our, conflictedFile);
                    if (mergeConflict.isExportOperation()) {
                        File tmp = ourFile;
                        ourFile = theirFile;
                        theirFile = tmp;
                    }
                } else {
                    String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                    String localName = conflictedFile.substring(rulesLocation.length());
                    our = userWorkspace.getLocalWorkspace().getRepository(repositoryId).read(localName);
                    ourFile = createTempFile(our, conflictedFile);
                }

                compare(Arrays.asList(theirFile, ourFile));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new Message(e.getMessage(), e);
        }
    }

    @Override
    public void setShowEqualElements(boolean showEqualElements) {
        super.setShowEqualElements(showEqualElements);
        clearTreeSelection();
        setDiffTree(getRichDiffTree().getDiffTreeNode());
    }

    private static void clearTreeSelection() {
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

        // Try new projectId-based storage first, fall back to legacy ConflictUtils for backward compatibility
        MergeConflictInfo mergeConflict = projectId != null
                ? conflictsSessionHolder.getConflictInfo(projectId)
                : ConflictUtils.getMergeConflict();
        if (mergeConflict != null) {
            var conflictDetails = mergeConflict.details();
            return conflictDetails.diffs().get(conflictedFile);
        }

        return null;
    }

    private static boolean isExcelFile(String conflictedFile) {
        return conflictedFile.endsWith(".xls") || conflictedFile.endsWith(".xlsx");
    }
}
