/**
 * Created on Sep 8, 2005
 *
 * $Id: JackRabbitUserTransaction.java,v 1.2 2007/03/06 21:45:55 costin Exp $
 * $Revision: 1.2 $
 */
package org.openl.rules.jacrkrabbit.transactions;

import javax.jcr.Session;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.jackrabbit.api.XASession;
import org.apache.jackrabbit.rmi.client.ClientXASession;

/**
 * JackRabbit User transaction (based on the XA Resource returned by JackRabbit).
 * <p/>
 * Inspired from JackRabbit test suite.
 * 
 * Internal {@link javax.transaction.UserTransaction} implementation.
 * 
 */
public class JackRabbitUserTransaction implements UserTransaction {

    /**
     * Global transaction id counter.
     * TODO: remove the static attribute 
     */
    private static byte counter = 0;

    /**
     * XAResource
     */

    private final XAResource xares;

    /**
     * Xid
     */
    private Xid xid;

    /**
     * Status
     */
    private int status = Status.STATUS_NO_TRANSACTION;

    /**
     * Create a new instance of this class. Takes a session as parameter.
     * 
     * @param session
     *            session. If session is not of type {@link XASession}, an
     *            <code>IllegalArgumentException</code> is thrown
     */
    public JackRabbitUserTransaction(Session session) {
        if (session instanceof XASession) {
            xares = ((XASession) session).getXAResource();
        } else if (session instanceof ClientXASession) {
            xares = (ClientXASession) session;
        } else {
            throw new IllegalArgumentException("Session not of type XASession");
        }
    }

    /**
     * @see javax.transaction.UserTransaction#begin
     */
    public void begin() throws NotSupportedException, SystemException {
        if (status != Status.STATUS_NO_TRANSACTION) {
            throw new IllegalStateException("Transaction already active");
        }

        try {
            xid = new XidImpl(counter++);
            xares.start(xid, XAResource.TMNOFLAGS);
            status = Status.STATUS_ACTIVE;

        } catch (XAException e) {

            throw new SystemException("Unable to begin transaction: " + "XA_ERR=" + e.errorCode);
        }
    }

    /**
     * @see javax.transaction.UserTransaction#commit
     */
    public void commit() throws IllegalStateException, RollbackException, SecurityException, SystemException {

        if (status != Status.STATUS_ACTIVE) {
            throw new IllegalStateException("Transaction not active");
        }

        try {
            xares.end(xid, XAResource.TMSUCCESS);

            status = Status.STATUS_PREPARING;
            xares.prepare(xid);
            status = Status.STATUS_PREPARED;

            status = Status.STATUS_COMMITTING;
            xares.commit(xid, false);
            status = Status.STATUS_COMMITTED;

        } catch (XAException e) {

            if (e.errorCode >= XAException.XA_RBBASE && e.errorCode <= XAException.XA_RBEND) {
                throw new RollbackException(e.toString());
            }

            throw new SystemException("Unable to commit transaction: " + "XA_ERR=" + e.errorCode);
        }
    }

    /**
     * @see javax.transaction.UserTransaction#getStatus
     */
    public int getStatus() throws SystemException {
        return status;
    }

    /**
     * @see javax.transaction.UserTransaction#rollback
     */
    public void rollback() throws IllegalStateException, SecurityException, SystemException {

        if (status != Status.STATUS_ACTIVE && status != Status.STATUS_MARKED_ROLLBACK) {

            throw new IllegalStateException("Transaction not active");
        }

        try {
            xares.end(xid, XAResource.TMFAIL);

            status = Status.STATUS_ROLLING_BACK;
            xares.rollback(xid);
            status = Status.STATUS_ROLLEDBACK;

        } catch (XAException e) {

            throw new SystemException("Unable to rollback transaction: " + "XA_ERR=" + e.errorCode);
        }
    }

    /**
     * @see javax.transaction.UserTransaction#setRollbackOnly()
     */
    public void setRollbackOnly() throws IllegalStateException, SystemException {
        if (status != Status.STATUS_ACTIVE) {
            throw new IllegalStateException("Transaction not active");
        }
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    /**
     * @see javax.transaction.UserTransaction#setTransactionTimeout
     */
    public void setTransactionTimeout(int seconds) throws SystemException {
    }

    /**
     * Internal {@link Xid} implementation.
     */
    class XidImpl implements Xid {

        /** Global transaction id */
        private final byte[] globalTxId;

        /**
         * Create a new instance of this class. Takes a global transaction
         * number as parameter
         * 
         * @param globalTxNumber
         *            global transaction number
         */
        public XidImpl(byte globalTxNumber) {
            this.globalTxId = new byte[] { globalTxNumber };
        }

        /**
         * @see javax.transaction.xa.Xid#getFormatId()
         */
        public int getFormatId() {
            return 0;
        }

        /**
         * @see javax.transaction.xa.Xid#getBranchQualifier()
         */
        public byte[] getBranchQualifier() {
            return new byte[0];
        }

        /**
         * @see javax.transaction.xa.Xid#getGlobalTransactionId()
         */
        public byte[] getGlobalTransactionId() {
            return globalTxId;
        }
    }

    /**
     * @return Returns the counter.
     */
    public byte getCounter() {
        return counter;
    }

    /**
     * @param counter The counter to set.
     */
    public void setCounter(byte counter) {
        JackRabbitUserTransaction.counter = counter;
    }
}
