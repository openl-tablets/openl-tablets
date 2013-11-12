package org.openl.rules.repository;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

public interface RTransactionManager {
    public static UserTransaction NO_TRANSACTION = new UserTransaction() {
        public void setTransactionTimeout(int arg0) throws SystemException {
        }

        public void setRollbackOnly() throws IllegalStateException, SystemException {
        }

        public void rollback() throws IllegalStateException, SecurityException, SystemException {
        }

        public int getStatus() throws SystemException {
            return 0;
        }

        public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException,
                SecurityException, IllegalStateException, SystemException {
        }

        public void begin() throws NotSupportedException, SystemException {
        }
    };
    UserTransaction getTransaction();
}
