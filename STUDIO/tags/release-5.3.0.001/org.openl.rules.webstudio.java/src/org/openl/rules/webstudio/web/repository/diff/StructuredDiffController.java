package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.diff.DiffElement;
import org.openl.rules.diff.StructuredDiff;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectVersion;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.rules.workspace.uw.UserWorkspaceProjectArtefact;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.faces.model.SelectItem;

/**
 * Supplies structured diff UI tree with data.
 *
 * @author Andrey Naumenko
 */
public class StructuredDiffController {
    private static Log log = LogFactory.getLog(StructuredDiffController.class);
    private List<DiffElement> diffElements = new ArrayList<DiffElement>();
    private String name;
    private String path;
    private String id;
    private String treeId;
    private UserWorkspace userWorkspace;
    private DesignTimeRepository designTimeRepository;
    private RepositoryTreeState repositoryTreeState;
    private Project project1;
    private Project project2;
    private StructuredDiffState diffState;

    public List<DiffElement> getDiffElements() {
        return diffElements;
    }

    public String getId() {
        if (id == null) {
            return treeId;
        }
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public Project getProject1() {
        return project1;
    }

    public Project getProject2() {
        return project2;
    }

    public String getTreeId() {
        return treeId;
    }

    public SelectItem[] getVersions() {
        UserWorkspaceProjectArtefact projectArtefact = (UserWorkspaceProjectArtefact) project1;
        Collection<ProjectVersion> versions = projectArtefact.getVersions();
        SelectItem[] selectItems = new SelectItem[versions.size()];

        int i = 0;
        for (ProjectVersion version : versions) {
            selectItems[i] = new SelectItem(version.getMajor() + "." + version.getMinor() + "." + version.getRevision());
            i++;
        }

        return selectItems;
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setDiffState(StructuredDiffState state) {
        diffState = state;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setInit(boolean init) {
        project1 = repositoryTreeState.getSelectedProject();

        try {
            project2 = designTimeRepository.getProject(project1.getName(),
                    new CommonVersionImpl(diffState.getVersion()));
        } catch (RepositoryException e) {
            log.error("Error reading project", e);
            return;
        }

        try {
            if (id != null) {
                path = id;
            } else {
                name = project1.getName();
                path = "/";
            }
            if ("1".equals(treeId)) {
                diffElements = StructuredDiff.getDiff(project1, project2, path, path);
            } else if ("2".equals(treeId)) {
                diffElements = StructuredDiff.getDiff(project2, project1, path, path);
            }
        } catch (ProjectException e) {
            log.error("", e);
        }
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setTreeId(String treeId) {
        this.treeId = treeId;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }
}
