package org.openl.rules.ui.repository;

import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ui.repository.beans.ProjectBean;
import org.openl.rules.ui.repository.handlers.*;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeRepository;
import org.openl.util.Log;

import java.util.List;

/**
 * User Session settings and handlers.
 * 
 * @author Aleh Bykhavets
 *
 */
public class UserSessionBean {
    private RRepository repository;
    private Context context;
    
    public UserSessionBean() {
        MessageQueue messages = new MessageQueue();

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            // TODO: log failure
            repository = new DummyRepository();
            messages.addMessage(e);
            Log.error("FACTORY", e);
        }

        context = new Context(repository, messages);

        FileHandler fileHandler = new FileHandler(context);
        FolderHandler folderHandler = new FolderHandler(context);
        ProjectHandler projectHandler = new ProjectHandler(context);

        context.setFileHandler(fileHandler);
        context.setFolderHandler(folderHandler);
        context.setProjectHandler(projectHandler);
    }

    public List<Error> getErrors() {
        return context.getMessageQueue().getAll();
    }

    public boolean clearMessages() {
        context.getMessageQueue().clear();
        return true;
    }


    public AbstractTreeNode getSelected() {
        return new TreeRepository(1, "rep");
    }

    // TODO refactor it: do not reveal it to UI
    public Context getContext() {
        return context;
    }
}
