package org.openl.rules.webstudio.web.repository.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.openl.rules.common.ProjectDependency;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.VersionInfo;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectFolder;
import org.openl.rules.project.abstraction.RulesProject;
import org.openl.rules.repository.api.ArtefactProperties;
import org.openl.rules.webstudio.web.repository.DependencyBean;
import org.openl.rules.webstudio.web.repository.RepositoryUtils;
import org.openl.rules.webstudio.web.repository.UiConst;
import org.openl.util.filter.IFilter;

public class TreeProductProject extends TreeProductFolder {
    private Map<Object, TreeNode> elements;
    private List<DependencyBean> dependencies;

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
        return "";
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
        if (projectVersion == null || this.getData().getVersionsCount() <= 2) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedAt() : null;
    }

    public String getModifiedBy() {
        ProjectVersion projectVersion = this.getData().getVersion();
        /* zero*/
        if (projectVersion == null || this.getData().getVersionsCount() <= 2) {
            return null;
        }

        VersionInfo vi = projectVersion.getVersionInfo();
        return (vi != null) ? vi.getCreatedBy() : null;
    }

    @Override
    public synchronized List<DependencyBean> getDependencies() {
        if (dependencies == null) {
            dependencies = new ArrayList<DependencyBean>();
        }

        return dependencies;
    }
    
    @Override
    public Map<Object, TreeNode> getElements() {
        if (elements == null && !isLeafOnly()) {
            elements = new LinkedHashMap<Object, TreeNode>();
            if (getData() instanceof AProjectFolder) {
                AProjectFolder folder = (AProjectFolder) getData();
                Collection<AProjectArtefact> filteredArtefacts = new ArrayList<AProjectArtefact>();
                for (AProjectArtefact artefact : folder.getArtefacts()) {
                    filteredArtefacts.add(artefact);
                }

                AProjectArtefact[] sortedArtefacts = new AProjectArtefact[filteredArtefacts.size()];
                sortedArtefacts = filteredArtefacts.toArray(sortedArtefacts);

                Arrays.sort(sortedArtefacts, RepositoryUtils.ARTEFACT_COMPARATOR);

                for (AProjectArtefact artefact : sortedArtefacts) {
                    addChild(artefact);
                }
            }
        }
        return elements;
    }

}
