package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.lock.LockInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.ProjectStatus;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.webstudio.web.repository.ProjectDescriptorArtefactResolver;
import org.openl.rules.repository.api.UserInfo;
import org.openl.rules.webstudio.web.repository.IFilter;
import org.openl.rules.webstudio.web.repository.UiConst;

/**
 * Represents OpenL project in a tree.
 *
 * @author Aleh Bykhavets
 */
public class TreeProject extends TreeFolder {

    private static final long serialVersionUID = -326805891782640894L;

    private final Logger log = LoggerFactory.getLogger(TreeProject.class);
    private final ProjectDescriptorArtefactResolver projectDescriptorResolver;
    private String logicalName;

    protected static String generateComments(UserWorkspaceProject userProject) {
        if (userProject.isLocalOnly()) {
            return "Local";
        }

        if (userProject.isDeleted()) {
            return "Deleted";
        }

        if (userProject.isOpenedForEditing()) {
            return null;
        }

        if (userProject.isOpened()) {
            String activeVersion = userProject.getFileData().getVersion();

            if (activeVersion != null && userProject.isOpenedOtherVersion()) {
                return "Revision " + activeVersion;
            }
        }

        LockInfo lock = userProject.getLockInfo();
        if (lock.isLocked()) {
            if (userProject.isLockedByMe(lock)) {
                return "The project is locked by yourself. Close this project.";
            } else {
                return "Locked by " + lock.getLockedBy();
            }
        }

        return null;
    }

    // ------ UI methods ------

    protected static String generateStatus(UserWorkspaceProject userProject) {
        ProjectStatus status = userProject.getStatus();

        if (status != ProjectStatus.EDITING && userProject.isLocked()) {
            if (status == ProjectStatus.VIEWING && !userProject.isLockedByMe()) {
                // To make it clear for a user, what is happening with project.
                status = ProjectStatus.VIEWING_VERSION;
            }
            return status.getDisplayValue() + ". Locked by " + userProject.getLockInfo().getLockedBy() + ".";
        }

        return status.getDisplayValue();
    }

    public TreeProject(String id,
                       String name,
                       IFilter<AProjectArtefact> filter,
                       ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        super(id, name, filter);
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    public String getComments() {
        return generateComments(getProject());
    }

    public Date getModifiedAt() {
        try {
            return getProject().getFileData().getModifiedAt();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    public String getModifiedBy() {
        try {
            return Optional.ofNullable(getProject().getFileData().getAuthor()).map(UserInfo::getName).orElse(null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Error";
        }
    }

    public String getEmailModifiedBy() {
        try {
            return Optional.ofNullable(getProject().getFileData().getAuthor()).map(UserInfo::getEmail).orElse(null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Error";
        }
    }

    @Override
    public String getIcon() {
        RulesProject project = getProject();

        if (project.isLocalOnly()) {
            return UiConst.ICON_PROJECT_LOCAL;
        }

        if (project.isDeleted()) {
            return UiConst.ICON_PROJECT_DELETED;
        }

        if (project.isModified()) {
            return UiConst.ICON_PROJECT_OPENED_FOR_EDITING;
        }

        boolean isLocked = project.isLocked();
        if (project.isOpened()) {
            if (isLocked) {
                return UiConst.ICON_PROJECT_OPENED_LOCKED;
            } else {
                return UiConst.ICON_PROJECT_OPENED;
            }
        } else {
            if (isLocked) {
                return UiConst.ICON_PROJECT_CLOSED_LOCKED;
            } else {
                return UiConst.ICON_PROJECT_CLOSED;
            }
        }
    }

    private RulesProject getProject() {
        return (RulesProject) getData();
    }

    public String getStatus() {
        return generateStatus(getProject());
    }

    @Override
    public String getType() {
        return UiConst.TYPE_PROJECT;
    }

    public String getVersion() {
        try {
            String projectVersion = getProject().getFileData().getVersion();
            return projectVersion == null ? UNVERSIONED : projectVersion;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "Error";
        }
    }

    public String getShortVersion() {
        String version = getVersion();
        if (UNVERSIONED.equals(version)) {
            return UNVERSIONED;
        }
        return version == null || version.length() < 6 ? version : version.substring(0, 6);
    }

    public boolean isRenamed() {
        return !getName().equals(getLogicalName());
    }

    public String getLogicalName() {
        if (logicalName == null) {
            logicalName = projectDescriptorResolver.getLogicalName(getProject());
        }
        return logicalName;
    }

    @Override
    public void refresh() {
        super.refresh();
        projectDescriptorResolver.deleteRevisionsFromCache(getProject());
        logicalName = null;
    }
}
