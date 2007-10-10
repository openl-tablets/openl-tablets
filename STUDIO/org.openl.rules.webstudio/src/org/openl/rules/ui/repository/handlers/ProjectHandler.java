package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.ui.repository.beans.AbstractEntityBean;
import org.openl.rules.repository.REntity;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.RVersion;
import org.openl.rules.repository.exceptions.RRepositoryException;

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
     * @param bean a project bean
     * @return list of elements
     */
    public List<AbstractEntityBean> getElements(ProjectBean bean) {
        RProject project = findRProject(bean);

        FolderHandler folderHandler = context.getFolderHandler();
        return folderHandler.listElements(project.getRootFolder());
    }

    /**
     * Adds new sub folder to existing folder.
     * 
     * @param bean Project UI Bean
     * @param newFolderName name of new sub folder
     * @return whether adding was successful
     */
    public boolean addFolder(ProjectBean bean, String newFolderName) {
        RProject project = findRProject(bean);
        
        FolderHandler folderHandler = context.getFolderHandler();
        return folderHandler.addFolder(project.getRootFolder(), newFolderName);
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
        } catch (RRepositoryException e) {
            context.getMessageQueue().addMessage(e);
        } catch (IndexOutOfBoundsException e) {
            // must have 1 version, at least
        } catch (NullPointerException e) {
            // must have version history
        }
        
        return pb;
    }
    
    private RProject findRProject(ProjectBean bean) {
        String id = bean.getId();
        REntity entity = getEntityById(id);
//        if (entity == null) ...
        RProject project = (RProject) entity;
        // check class cast exception
        
        return project;
    }
}
