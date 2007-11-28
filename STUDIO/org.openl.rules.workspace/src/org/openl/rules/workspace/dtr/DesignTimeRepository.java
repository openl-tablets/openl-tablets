package org.openl.rules.workspace.dtr;

import java.util.List;

import org.openl.rules.repository.CommonVersion;
import org.openl.rules.workspace.WorkspaceUser;
import org.openl.rules.workspace.abstracts.Project;
import org.openl.rules.workspace.abstracts.ProjectException;
import org.openl.rules.workspace.abstracts.ProjectsContainer;

public interface DesignTimeRepository extends ProjectsContainer<RepositoryProject> {
    /**
     * Gets particular version of a project.
     * 
     * @param name name of project
     * @param version exact version of project
     * @return specified version of project
     * @throws RepositoryException
     */
    RepositoryProject getProject(String name, CommonVersion version) throws RepositoryException;

    /**
     * Updates project in Design Time Repository.
     * WorkspaceUser parameter should guarantee that project is updated by user 
     * who is locking the project.
     * Project can be of any implementation -- LocalWorkspaceProject,
     * or even older version of the project.
     * DTR will take name of <code>project</code> argument and update 
     * a DTR project with the same name.
     * 
     * @param project
     * @param user
     * @throws RepositoryException
     */
    void updateProject(Project project, WorkspaceUser user, int major, int minor) throws RepositoryException;
    
    /**
     * Copies project in/into Design Time Repository.
     * Project can be -- LocalWorkspaceProject, a version of project in DTR,
     * any other class that implements Project interface.
     * 
     * @param project source project
     * @param name name of new project, must be unique
     * @throws RepositoryException
     */
    void copyProject(Project project, String name, WorkspaceUser user) throws ProjectException;
    
    void createProject(String name) throws RepositoryException;
    
    public RepositoryDDProject getDDProject(String name) throws RepositoryException;
    public List<RepositoryDDProject> getDDProjects() throws RepositoryException;
    public void createDDProject(String name) throws RepositoryException;
}
