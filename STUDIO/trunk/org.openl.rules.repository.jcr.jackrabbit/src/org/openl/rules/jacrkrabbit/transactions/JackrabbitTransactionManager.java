package org.openl.rules.jacrkrabbit.transactions;

import javax.jcr.Session;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openl.rules.repository.RTransactionManager;

public class JackrabbitTransactionManager implements RTransactionManager {
    private static Log LOG = LogFactory.getLog(JackrabbitTransactionManager.class);
    private Session session;

    public JackrabbitTransactionManager(Session session) {
        this.session = session;
    }

    public UserTransaction getTransaction() {
        try {
            return new JackRabbitUserTransaction(session);
        } catch (Exception e) {
            LOG.warn("Failed to create jackrabbit transaction.", e);
            return NO_TRANSACTION;
        }
    }
}
