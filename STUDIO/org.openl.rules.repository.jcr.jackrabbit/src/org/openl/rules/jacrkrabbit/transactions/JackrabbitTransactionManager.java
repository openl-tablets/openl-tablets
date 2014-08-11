package org.openl.rules.jacrkrabbit.transactions;

import org.openl.rules.repository.RTransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.transaction.UserTransaction;

public class JackrabbitTransactionManager implements RTransactionManager {
    private final Logger log = LoggerFactory.getLogger(JackrabbitTransactionManager.class);
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
            log.warn("Failed to create jackrabbit transaction.", e);
            return NO_TRANSACTION;
        }
    }
}
