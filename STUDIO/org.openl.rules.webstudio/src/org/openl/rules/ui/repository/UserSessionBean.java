package org.openl.rules.ui.repository;

import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.ui.repository.handlers.*;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;

import java.util.List;

public class UserSessionBean {
    private RRepository repository;
    private Context context;
    private RepositoryHandler repositoryHandler;
    private RepositoryTreeHandler repositoryTree;

    public UserSessionBean() {
        MessageQueue messages = new MessageQueue();

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            // TODO: log failure
            repository = new DummyRepository();
            messages.addMessage(e);
        }

        context = new Context(repository, messages);

        FileHandler fileHandler = new FileHandler(context);
        FolderHandler folderHandler = new FolderHandler(context);
        ProjectHandler projectHandler = new ProjectHandler(context);
        repositoryHandler = new RepositoryHandler(context);

        context.setFileHandler(fileHandler);
        context.setFolderHandler(folderHandler);
        context.setProjectHandler(projectHandler);
        context.setRepositoryHandler(repositoryHandler);
        
        repositoryTree = new RepositoryTreeHandler(context);
    }

    public List<ProjectBean> getProjects() {
        return repositoryHandler.getProjects();
    }

    public List<Error> getErrors() {
        return context.getMessageQueue().getAll();
    }

    public boolean clearMessages() {
        context.getMessageQueue().clear();
        return true;
    }

    public RepositoryTreeHandler getRepositoryTree() {
        return repositoryTree;
    }
    
    public AbstractTreeNode getSelected() {
        return repositoryTree.getSelected();
    }
    
    private boolean openModal;
    public boolean getOpenModal() {
        boolean r = openModal;
        openModal = false;
        return r;
    }
    public void openDelModal() {
        openModal = true;
    }
}
