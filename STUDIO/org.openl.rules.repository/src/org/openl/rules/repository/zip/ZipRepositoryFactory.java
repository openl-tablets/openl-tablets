package org.openl.rules.repository.zip;

import java.util.function.Consumer;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.api.Repository;

/**
 * Zipped local file system repository factory.
 *
 * @author Yury Molchan
 */
public class ZipRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-zip";

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
        ZippedLocalRepository repository = new ZippedLocalRepository();
        initParamsCallback.accept(repository);
        repository.initialize();
        return repository;
    }
}
