package org.openl.rules.repository.db;

import java.util.function.Function;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;

/**
 * Database repository factory with connection using Datasource JNDI resource.
 *
 * @author Yury Molchan
 */
public class DatasourceDBRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-jndi";
    private static final String OLD_ID = "org.openl.rules.repository.db.DatasourceDBRepositoryFactory";

    @Override
    public boolean accept(String factoryID) {
        return factoryID.equals(ID) || factoryID.equals(OLD_ID);
    }

    @Override
    public String getRefID() {
        return ID;
    }

    @Override
    public Repository create(Function<String, String> settings) {
        DatasourceDBRepository repository = new DatasourceDBRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
