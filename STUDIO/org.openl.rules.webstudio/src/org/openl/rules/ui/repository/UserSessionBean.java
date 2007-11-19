package org.openl.rules.ui.repository;

import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RulesRepositoryFactory;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.ui.repository.tree.AbstractTreeNode;
import org.openl.rules.ui.repository.tree.TreeRepository;

import org.openl.util.Log;

import java.util.LinkedList;
import java.util.List;


/**
 * User Session settings and handlers.
 *
 * @author Aleh Bykhavets
 */
public class UserSessionBean {
    private RRepository repository;
    private List<String> messages = new LinkedList<String>();

    public UserSessionBean() {

        try {
            repository = RulesRepositoryFactory.getRepositoryInstance();
        } catch (RRepositoryException e) {
            // TODO: log failure
            repository = new DummyRepository();
            //messages.addMessage(e);
            Log.error("FACTORY", e);
        }
    }

    public List<String> getMessages() {
        return messages;
    }

    public AbstractTreeNode getSelected() {
        return new TreeRepository(1, "rep");
    }
}
