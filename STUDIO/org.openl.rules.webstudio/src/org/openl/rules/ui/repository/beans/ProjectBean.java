package org.openl.rules.ui.repository.beans;

import org.openl.rules.ui.repository.handlers.ProjectHandler;

import java.util.List;

public class ProjectBean extends AbstractEntityBean {

    public List<AbstractEntityBean> getElements() {
        ProjectHandler projectHandler = (ProjectHandler) getHandler();
        return projectHandler.getElements(this);
    }
}
