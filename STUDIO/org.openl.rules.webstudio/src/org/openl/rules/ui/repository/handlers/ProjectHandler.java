package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.CannotFindEntityException;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.LinkedList;
import java.util.List;

/**
 * Handler for Project UI Bean.
 * 
 * @author Aleh Bykhavets
 *
 */
public class ProjectHandler extends BeanHandler {

    public ProjectHandler(Context context) {
        super(context);
    }

    /**
     * Gets list of elements in a root folder of a repository project.
     * 
     * @param bean a project controllers
     * @return list of elements
     */
    public List<AbstractEntityBean> getElements(ProjectBean bean) {
        try {
            RProject project = getRProject(bean);
            FolderHandler folderHandler = context.getFolderHandler();
            return folderHandler.listElements(project.getRootFolder());
        } catch (CannotFindEntityException e) {
            context.getMessageQueue().addMessage(e);
            return new LinkedList<AbstractEntityBean>();
        }        
    }

    /**
     * Adds new sub folder to existing folder.
     * 
     * @param bean Project UI Bean
     * @param newFolderName name of new sub folder
     * @return whether adding was successful
     */
    public boolean addFolder(ProjectBean bean, String newFolderName) {
        try {
            RProject project = getRProject(bean);
            FolderHandler folderHandler = context.getFolderHandler();
            return folderHandler.addFolder(project.getRootFolder(),
                    newFolderName);
        } catch (CannotFindEntityException e) {
            context.getMessageQueue().addMessage(e);
            return false;
        }        
    }
    
    public void undelete(ProjectBean bean) {
        try {
            RProject project = getRProject(bean);
            project.undelete();
        } catch (Exception e) {
            // TODO add 2 log
            context.getMessageQueue().addMessage(e);
        } finally {
            context.refresh();
        }
    }
    
    public void erase(ProjectBean bean) {
        try {
            RProject project = getRProject(bean);
            if (project.isMarked4Deletion()) {
                project.erase();
            } else {
                // TODO cannot erase the project, it must be marked 4 deletion first
            }
        } catch (Exception e) {
            // TODO add 2 log
            context.getMessageQueue().addMessage(e);
        } finally {
            context.refresh();
        }
    }

    /**
     * Creates UI Bean for a repository project.
     * 
     * @param project a repository project
     * @return new Project UI Bean
     */
    protected ProjectBean createBean(RProject project) {
        ProjectBean pb = new ProjectBean();
        initBean(pb, project);
        
        try {
            RVersion first = project.getVersionHistory().get(0);
            pb.setCreated(first.getCreated());

            pb.setMarked4Deletion(project.isMarked4Deletion());
        } catch (RRepositoryException e) {
            context.getMessageQueue().addMessage(e);
        } catch (IndexOutOfBoundsException e) {
            // must have 1 version, at least
        } catch (NullPointerException e) {
            // must have version history
        }
        
        return pb;
    }
    
    private RProject getRProject(ProjectBean bean) throws CannotFindEntityException {
        String id = bean.getId();
        REntity entity = getEntityById(id);
        RProject project = (RProject) entity;
        
        return project;
    }
}
