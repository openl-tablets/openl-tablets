package org.openl.rules.webstudio.web.repository.tree;

import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.*;
import org.openl.rules.project.resolving.ProjectDescriptorArtefactResolver;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.openl.rules.webstudio.filter.IFilter;

import java.util.Date;

/**
 * Represents OpenL project in a tree.
 *
 * @author Aleh Bykhavets
 *
 */
public class TreeProject extends TreeFolder {

    private static final long serialVersionUID = -326805891782640894L;

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
            ProjectVersion activeVersion = userProject.getVersion();

            if (activeVersion != null && userProject.isOpenedOtherVersion()) {
                return "Revision " + activeVersion.getVersionName();
            } else {
                return null;
            }
        }

        if (userProject.isLocked()) {
            LockInfo lock = userProject.getLockInfo();

            if (lock != null) {
                if (userProject.isLockedByMe()) {
                    return "Locked by you. Please close this project.";
                } else {
                    return "Locked by " + lock.getLockedBy().getUserName();
                }
            }
        }

        return null;
    }

    // ------ UI methods ------

    protected static String generateStatus(UserWorkspaceProject userProject) {
        ProjectStatus status = userProject.getStatus();

        if (status != ProjectStatus.EDITING && userProject.isLocked()) {
            return status.getDisplayValue() + " - Locked";
        }

        return status.getDisplayValue();
    }

    public TreeProject(String id, String name, IFilter<AProjectArtefact> filter, ProjectDescriptorArtefactResolver projectDescriptorResolver) {
        super(id, name, filter);
        this.projectDescriptorResolver = projectDescriptorResolver;
    }

    public String getComments() {
        return generateComments(getProject());
    }

    public Date getCreatedAt() {
        ProjectVersion projectVersion = getProject().getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getCreatedBy() {
        ProjectVersion projectVersion = (getProject()).getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

    public Date getModifiedAt() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null || getProject().getVersionsCount() <= 2) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getModifiedBy() {
        ProjectVersion projectVersion = (getProject()).getVersion();
        /* zero*/
        if (projectVersion == null || getProject().getVersionsCount() <= 2) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
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

        if (project.isOpenedForEditing()) {
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
        ProjectVersion projectVersion = (getProject()).getVersion();
        if (projectVersion == null) {
            return "unversioned";
        }
        return projectVersion.getVersionName();
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
