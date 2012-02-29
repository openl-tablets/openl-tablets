package org.openl.rules.repository.factories;

import javax.jcr.Session;

import org.openl.rules.jacrkrabbit.transactions.JackrabbitTransactionManager;
import org.openl.rules.repository.RTransactionManager;

public abstract class AbstractJackrabbitRepositoryFactory extends AbstractJcrRepositoryFactory {
    public RTransactionManager getTrasactionManager(Session session) {
        return new JackrabbitTransactionManager(session);
    }

}
