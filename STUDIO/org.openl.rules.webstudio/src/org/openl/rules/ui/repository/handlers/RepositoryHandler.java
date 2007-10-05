package org.openl.rules.ui.repository.handlers;

import org.openl.rules.ui.repository.Context;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.repository.RProject;
import org.openl.rules.repository.exceptions.RRepositoryException;

import java.util.List;
import java.util.LinkedList;

public class RepositoryHandler {
    private Context context;

    public RepositoryHandler(Context context) {
        this.context = context;
    }

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
}
