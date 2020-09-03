package org.openl.rules.repository.api;

import java.io.Closeable;
import java.util.Date;
import java.util.Objects;

public class RepositorySettings implements Closeable {
    private Repository repository;
    private Date syncDate = new Date();

    public RepositorySettings(Repository repository) {
        Objects.requireNonNull(repository);
        this.repository = repository;

        repository.setListener(() -> syncDate = new Date());
    }

    public Repository getRepository() {
        return repository;
    }

    public Date getSyncDate() {
        return syncDate;
    }

    @Override
    public void close() {
        if (repository != null) {
            repository.setListener(null);
            repository = null;
        }
    }
}
