package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.ProjectHandler;

import java.util.List;

import javax.naming.Context;

/**
 * UI Bean for Project.
 * 
 * @author Aleh Bykhavets
 *
 */
public class ProjectBean extends AbstractEntityBean {
    private List<AbstractEntityBean> elements;
    private boolean isMarked4Deletion;
    
    /** {@inheritDoc} */
    public List<AbstractEntityBean> getElements() {
        if (elements == null) {
            ProjectHandler projectHandler = (ProjectHandler) getHandler();
            elements = projectHandler.getElements(this);
        }
        
        return elements;
    }
    
    public boolean getMarked4Deletion() {
        return isMarked4Deletion;
    }
    
    public void setMarked4Deletion(boolean marked) {
        isMarked4Deletion = marked;
    }
    
    public void undelete() {
        ProjectHandler projectHandler = (ProjectHandler) getHandler();
        projectHandler.undelete(this);
    }
    
    public void erase() {
        ProjectHandler projectHandler = (ProjectHandler) getHandler();
        projectHandler.erase(this);
    }

    /** {@inheritDoc} */
    public String getType() {
        return "project";
    }
}
