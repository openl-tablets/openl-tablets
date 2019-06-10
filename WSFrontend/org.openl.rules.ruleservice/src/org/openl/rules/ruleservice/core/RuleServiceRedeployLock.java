package org.openl.rules.ruleservice.core;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class RuleServiceRedeployLock {
    private static class RuleServiceRedeployLockHolder {
        private static final RuleServiceRedeployLock INSTANCE = new RuleServiceRedeployLock();
    }

    public static RuleServiceRedeployLock getInstance() {
        return RuleServiceRedeployLockHolder.INSTANCE;
    }

    private ReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();

    public Lock getReadLock() {
        return reentrantReadWriteLock.readLock();
    }

    public Lock getWriteLock() {
        return reentrantReadWriteLock.writeLock();
    }

}
