package org.openl.rules.webstudio.web.repository.merge;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;

import javax.annotation.PreDestroy;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;

@ManagedBean
@SessionScoped
public class ConflictedFileDiffController extends ExcelDiffController {
    private final Logger log = LoggerFactory.getLogger(ConflictedFileDiffController.class);

    @ManagedProperty(value = "#{workspaceManager}") private MultiUserWorkspaceManager workspaceManager;

    public void setConflictedFile(String conflictedFile) {
        try {
            deleteTempFiles();
            setDiffTree(null);

            if (StringUtils.isBlank(conflictedFile)) {
                return;
            }

            MergeConflictInfo mergeConflict = getMergeConflict();
            if (mergeConflict != null) {
                MergeConflictException exception = mergeConflict.getException();

                WorkspaceUser user = new WorkspaceUserImpl(SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName());
                UserWorkspace userWorkspace = workspaceManager.getUserWorkspace(user);

                FileItem their = userWorkspace.getDesignTimeRepository()
                    .getRepository()
                    .readHistory(conflictedFile, exception.getTheirCommit());
                File theirFile = createTempFile(their, conflictedFile);

                String rulesLocation = userWorkspace.getDesignTimeRepository().getRulesLocation();
                String localName = conflictedFile.substring(rulesLocation.length());
                FileItem our = userWorkspace.getLocalWorkspace().getRepository().read(localName);
                File ourFile = createTempFile(our, localName);

                compare(Arrays.asList(ourFile, theirFile));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ValidationException(e.getMessage(), e);
        }
    }

    private File createTempFile(FileItem item, String fullName) throws FileNotFoundException {
        if (item == null) {
            throw new FileNotFoundException("File " + fullName + " is not found");
        }
        File ourFile = FileTool.toTempFile(item.getStream(), FileUtils.getName(fullName));
        if (ourFile == null) {
            throw new FileNotFoundException("Can't create temp file for '" + fullName + "'");
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
        setFilesToCompare(Collections.<File>emptyList());
        deleteTempFiles();
        setDiffTree(null);
    }

    public void setWorkspaceManager(MultiUserWorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    private MergeConflictInfo getMergeConflict() {
        return (MergeConflictInfo) FacesUtils.getSessionMap().get(Constants.SESSION_PARAM_MERGE_CONFLICT);
    }
}
