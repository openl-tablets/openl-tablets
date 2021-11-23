package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;
import java.util.Optional;

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

    public Date getModifiedAt() {
        return getVersionInfo().map(VersionInfo::getCreatedAt).orElse(null);
    }

    public String getModifiedBy() {
        return getVersionInfo().map(VersionInfo::getCreatedBy).orElse(null);
    }

    public String getEmailModifiedBy() {
        return getVersionInfo().map(VersionInfo::getEmailCreatedBy).orElse(null);
    }

    private Optional<VersionInfo> getVersionInfo() {
        return Optional.ofNullable(getProject().getVersion()).map(ProjectVersion::getVersionInfo);
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
            return UNVERSIONED;
        }
        return projectVersion.getVersionName();
    }

    public String getShortVersion() {
        String version = getVersion();
        if (UNVERSIONED.equals(version)) {
            return UNVERSIONED;
        }
        return version == null || version.length() < 6 ? version : version.substring(0, 6);
    }
}
