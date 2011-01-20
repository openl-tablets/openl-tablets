package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeDProject extends TreeFile {
    private static final long serialVersionUID = -1058464776132912419L;

    public TreeDProject(String id, String name) {
        super(id, name);
    }

    // ------ UI methods ------

    public String getComments() {
        return TreeProject.generateComments(getData());
    }

    public Date getCreatedAt() {
        ProjectVersion projectVersion = getProject().getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getCreatedBy() {
        ProjectVersion projectVersion = (getProject()).getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

    @Override
    public String getIconLeaf() {
        ADeploymentProject project = (ADeploymentProject) getData();

        if (project.isLocalOnly()) {
            return UiConst.ICON_PROJECT_LOCAL;
        }

        if (project.isDeleted()) {
            return UiConst.ICON_PROJECT_DELETED;
        }

        if (project.isCheckedOut()) {
            return UiConst.ICON_PROJECT_CHECKED_OUT;
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

    private AProject getProject() {
        return (AProject) getData();
    }

    public String getStatus() {
        return TreeProject.generateStatus(getData());
    }

    @Override
    public String getType() {
        return UiConst.TYPE_DEPLOYMENT_PROJECT;
    }

    public String getVersion() {
        ProjectVersion projectVersion = (getProject()).getVersion();
        if (projectVersion == null) {
            return "unversioned";
        }
        return projectVersion.getVersionName();
    }
}
