package org.openl.rules.repository.azure;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;

import java.util.function.Function;

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
    public Repository create(Function<String, String> settings) {
        AzureBlobRepository repository = new AzureBlobRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
