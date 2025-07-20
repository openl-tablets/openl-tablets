package org.openl.rules.webstudio.web.repository.tree;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.abstraction.IProject;
import org.openl.rules.webstudio.web.repository.IFilter;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.UiConst;

public class TreeProductionDProject extends TreeProductFolder {
    private final IFilter<AProjectArtefact> filter;

    public TreeProductionDProject(String id, String name, IFilter<AProjectArtefact> filter) {
        super(id, name, filter);
        this.filter = filter;
    }

    private Map<Object, TreeNode> elements;

    @Override
    public String getType() {
        return UiConst.TYPE_PRODUCTION_DEPLOYMENT_PROJECT;
    }

    @Override
    public String getIconLeaf() {
        return UiConst.ICON_PROJECT_CLOSED;
    }

    @Override
    public Map<Object, TreeNode> getElements() {
        if (elements == null && !isLeafOnly()) {
            elements = new LinkedHashMap<>();

            Collection<IProject> prjList = ((Deployment) getData()).getProjects();
            AProjectArtefact[] sortedArtefacts = new AProjectArtefact[prjList.size()];
            int i = 0;
            for (IProject iProject : prjList) {
                sortedArtefacts[i++] = (AProjectArtefact) iProject;
            }

            Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);

            for (AProjectArtefact apa : sortedArtefacts) {
                addChild(apa);
            }

        }
        return elements;
    }

    @Override
    public boolean isLeaf() {
        // If elements aren't initialized, consider it as not leaf
        return isLeafOnly() || elements != null && elements.isEmpty();
    }

    @Override
    public void addChild(AProjectArtefact childArtefact) {
        String name = childArtefact.getName();
        String id = RepositoryUtils.getTreeNodeId(childArtefact);
        if (childArtefact instanceof AProjectFolder) {
            TreeProductProject prj = new TreeProductProject(id, name, filter);
            prj.setData(childArtefact);

            add(prj);
        }
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

    private AProjectFolder getProject() {
        return (AProjectFolder) getData();
    }

}
