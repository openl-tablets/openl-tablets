package org.openl.rules.repository.azure;

import java.util.function.Consumer;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.api.Repository;

public class AzureBlobRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-azure-blob";

    @Override
    public boolean accept(String factoryID) {
        return factoryID.equals(ID);
    }

    @Override
    public String getRefID() {
        return ID;
    }

    @Override
    public Repository create(Consumer<Repository> initParamsCallback) {
        AzureBlobRepository repository = new AzureBlobRepository();
        initParamsCallback.accept(repository);
        repository.initialize();
        return repository;
    }
}
