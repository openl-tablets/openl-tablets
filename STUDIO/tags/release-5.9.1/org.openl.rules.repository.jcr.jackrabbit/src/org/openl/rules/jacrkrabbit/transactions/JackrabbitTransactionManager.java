package org.openl.rules.jacrkrabbit.transactions;

import javax.jcr.Session;
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
            //jackrabbit transaction does not work correctly. Strange NPE is appears.
            //TODO: use jackrabbit transaction after : https://issues.apache.org/jira/browse/JCR-2581
            //return new JackRabbitUserTransaction(session);
            return NO_TRANSACTION;
        } catch (Exception e) {
            LOG.warn("Failed to create jackrabbit transaction.", e);
            return NO_TRANSACTION;
        }
    }
}
