package org.openl.rules.repository.azure;

import java.util.function.UnaryOperator;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
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
    public Repository create(UnaryOperator<String> settings) {
        var repository = new AzureBlobRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
