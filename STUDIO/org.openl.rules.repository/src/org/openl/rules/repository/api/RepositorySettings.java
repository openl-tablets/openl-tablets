package org.openl.rules.repository.api;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import lombok.Getter;

import org.openl.rules.lock.LockManager;

public class RepositorySettings implements Closeable {
    @Getter
    private Repository repository;
    @Getter
    private Date syncDate = new Date();
    private final LockManager lockManager;
    private final int lockTimeToLive;

    public RepositorySettings(Repository repository, String locksRoot, int lockTimeToLive) {
        Objects.requireNonNull(repository);
        this.repository = repository;
        this.lockManager = new LockManager(Path.of(locksRoot));
        this.lockTimeToLive = lockTimeToLive;

        repository.setListener(() -> syncDate = new Date());
    }

    public void lock(String fileName) throws IOException {
        var locked = lockManager.getLock(fileName).forceLock("", lockTimeToLive, TimeUnit.SECONDS);
        if (!locked) {
            throw new IOException("Cannot create a lock for '" + fileName + "'");
        }
    }

    public void unlock(String fileName) {
        lockManager.getLock(fileName).unlock();
    }

    @Override
    public void close() {
        if (repository != null) {
            repository.setListener(null);
            repository = null;
        }
    }
}
