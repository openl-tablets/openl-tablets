package org.openl.rules.repository.api;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.openl.rules.lock.LockManager;

public class RepositorySettings implements Closeable {
    private Repository repository;
    private Date syncDate = new Date();
    private final LockManager lockManager;
    private final int lockTimeToLive;

    public RepositorySettings(Repository repository, String locksRoot, int lockTimeToLive) {
        Objects.requireNonNull(repository);
        this.repository = repository;
        this.lockManager = new LockManager(Paths.get(locksRoot));
        this.lockTimeToLive = lockTimeToLive;

        repository.setListener(() -> syncDate = new Date());
    }

    public Repository getRepository() {
        return repository;
    }

    public Date getSyncDate() {
        return syncDate;
    }

    public void lock(String fileName) throws IOException {
        boolean locked = lockManager.getLock(fileName).forceLock("", lockTimeToLive, TimeUnit.SECONDS);
        if (!locked) {
            throw new IOException("Can't create a lock for '" + fileName + "'");
        }
    }

    public void unlock(String fileName) {
        lockManager.getLock(fileName).forceUnlock();
    }

    @Override
    public void close() {
        if (repository != null) {
            repository.setListener(null);
            repository = null;
        }
    }
}
