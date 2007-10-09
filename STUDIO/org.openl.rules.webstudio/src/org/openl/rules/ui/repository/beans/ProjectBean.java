package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.ProjectHandler;

import java.util.List;

/**
 * UI Bean for Project.
 * 
 * @author Aleh Bykhavets
 *
 */
public class ProjectBean extends AbstractEntityBean {
    private List<AbstractEntityBean> elements;
    
    /** {@inheritDoc} */
    public List<AbstractEntityBean> getElements() {
        if (elements == null) {
            ProjectHandler projectHandler = (ProjectHandler) getHandler();
            elements = projectHandler.getElements(this);
        }
        
        return elements;
    }
}
