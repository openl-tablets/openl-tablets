package org.openl.rules.webstudio.web.repository.diff;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.openl.rules.diff.DiffElement;
import org.openl.rules.diff.StructuredDiff;
import org.openl.rules.repository.CommonVersionImpl;
import org.openl.rules.webstudio.web.repository.RepositoryTreeState;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.rules.workspace.dtr.RepositoryException;
import org.openl.rules.workspace.uw.UserWorkspace;

import java.util.List;


/**
 * Server-side for providing JavaScript flexTree with structured diff data.
 *
 * @author Andrey Naumenko
 */
public class DiffController {
    private static Log log = LogFactory.getLog(DiffController.class);
    private List<DiffElement> diffElements;
    private String path;
    private UserWorkspace userWorkspace;
    private DesignTimeRepository designTimeRepository;
    private RepositoryTreeState repositoryTreeState;
    private Project project1;
    private Project project2;

    public List<DiffElement> getDiffElements() {
        return diffElements;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Project getProject1() {
        return project1;
    }

    public Project getProject2() {
        return project2;
    }

    public void setInit(boolean init) {
        project1 = repositoryTreeState.getSelectedProject();

        try {
            project2 = designTimeRepository.getProject(project1.getName(),
                    new CommonVersionImpl("0.0.1"));
        } catch (RepositoryException e) {
            log.error("Error reading project", e);
            return;
        }

        try {
            diffElements = StructuredDiff.getDiff(project1, project2, path, path);
        } catch (ProjectException e) {
            log.error("", e);
        }
    }

    public void setDesignTimeRepository(DesignTimeRepository designTimeRepository) {
        this.designTimeRepository = designTimeRepository;
    }

    public void setUserWorkspace(UserWorkspace userWorkspace) {
        this.userWorkspace = userWorkspace;
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }
}
