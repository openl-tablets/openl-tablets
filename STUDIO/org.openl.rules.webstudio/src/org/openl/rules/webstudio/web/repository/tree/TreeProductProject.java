package org.openl.rules.webstudio.web.repository.tree;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

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
        return Optional.ofNullable(this.getData().getVersion()).map(ProjectVersion::getVersionInfo);
    }
}
