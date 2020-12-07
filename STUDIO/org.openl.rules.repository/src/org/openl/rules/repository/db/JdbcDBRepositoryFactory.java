package org.openl.rules.repository.db;

import java.util.function.Function;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;

/**
 * Database repository factory with connection using JDBC url.
 *
 * @author Yury Molchan
 */
public class JdbcDBRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-jdbc";
    private static final String OLD_ID = "org.openl.rules.repository.db.JdbcDBRepositoryFactory";

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
        JdbcDBRepository repository = new JdbcDBRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
