package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeDProject extends TreeFile {
    private static final long serialVersionUID = -1058464776132912419L;

    public TreeDProject(String id, String name) {
        super(id, name);
    }

    // ------ UI methods ------

    public String getComments() {
        return TreeProject.generateComments(getProject());
    }

    public Date getCreatedAt() {
        ProjectVersion projectVersion = getProject().getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedAt() : null;
    }

    public String getCreatedBy() {
        ProjectVersion projectVersion = getProject().getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedBy() : null;
    }

    public Date getModifiedAt() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedAt() : null;
    }

    public String getModifiedBy() {
        ProjectVersion projectVersion = getProject().getVersion();
        /* zero */
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return vi != null ? vi.getCreatedBy() : null;
    }

    @Override
    public String getIconLeaf() {
        ADeploymentProject project = getProject();

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

    private ADeploymentProject getProject() {
        return (ADeploymentProject) getData();
    }

    public String getStatus() {
        return TreeProject.generateStatus(getProject());
    }

    @Override
    public String getType() {
        return UiConst.TYPE_DEPLOYMENT_PROJECT;
    }

    public String getVersion() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null) {
            return "unversioned";
        }
        return projectVersion.getVersionName();
    }
}
