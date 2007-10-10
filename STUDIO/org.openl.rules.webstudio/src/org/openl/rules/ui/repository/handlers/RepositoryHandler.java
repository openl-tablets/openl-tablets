package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;
import java.util.LinkedList;

/**
 * Repository Handler.
 * It works with repository projects.
 * 
 * @author Aleh Bykhavets
 *
 */
public class RepositoryHandler {
    private Context context;

    public RepositoryHandler(Context context) {
        this.context = context;
    }

    /**
     * Gets all projects from a rule repository.
     * 
     * @return list of projects
     */
    public List<ProjectBean> getProjects() {
        List<ProjectBean> result = new LinkedList<ProjectBean>();

        try {
            ProjectHandler projectHandler = context.getProjectHandler();
            for (RProject project : context.getRepository().getProjects()) {
                ProjectBean pb = projectHandler.createBean(project);
                result.add(pb);
            }
        } catch (RRepositoryException e) {
            // TODO: log exception
            context.getMessageQueue().addMessage(e);
        }

        return result;
    }
    
    public boolean addProject(String newProjectName) {
        boolean result;
        
        try {
            context.getRepository().createProject(newProjectName);
            result = true;
        } catch (RRepositoryException e) {
            // TODO log exception
            // failed to create new project
            context.getMessageQueue().addMessage(e);
            result = false;
        }
        
        // TODO !!! Refresh tree !!!
        // context.refresh();
        
        return result;
    }
    
    public boolean copyProject(String existingProject, String newProject) {
        boolean result;
        
        try {
            context.getRepository().createProject(newProject);
            // TODO copy content ... somehow... or let RAL do that
            // context.getRepository().copyProject(RProject, String);
            result = true;
        } catch (RRepositoryException e) {
            // TODO log exception
            // failed to create new copy project
            // failed to copy content of project
            context.getMessageQueue().addMessage(e);
            result = false;
        }
        
        // TODO !!! Refresh tree !!!
        // context.refresh();

        return result;
    }
}
