package org.openl.rules.repository.zip;

import java.util.function.Function;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
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
    public Repository create(Function<String, String> settings) {
        ZippedLocalRepository repository = new ZippedLocalRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
