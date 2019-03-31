package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.webstudio.filter.IFilter;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductProject extends TreeProductFolder {
    private Map<Object, TreeNode> elements;

    public TreeProductProject(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name, filter);
    }

    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_PROJECT;
    }

    @Override
    public String getIcon() {
        return UiConst.ICON_PROJECT_CLOSED;
    }

    public String getStatus() {
        return StringUtils.EMPTY;
    }

    public Date getCreatedAt() {
        ProjectVersion projectVersion = this.getData().getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getCreatedBy() {
        ProjectVersion projectVersion = this.getData().getFirstVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

    public Date getModifiedAt() {
        ProjectVersion projectVersion = this.getData().getVersion();
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getModifiedBy() {
        ProjectVersion projectVersion = this.getData().getVersion();
        /* zero*/
        if (projectVersion == null) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

}
