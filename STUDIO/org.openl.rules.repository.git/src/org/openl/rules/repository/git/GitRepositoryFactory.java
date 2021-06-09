package org.openl.rules.repository.git;

import java.util.function.Function;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.RepositoryInstatiator;
import org.openl.rules.repository.api.Repository;

/**
 * Git repository factory.
 *
 * @author Yury Molchan
 */
public class GitRepositoryFactory implements RepositoryFactory {
    private static final String ID = "repo-git";
    private static final String OLD_ID = "org.openl.rules.repository.git.GitRepository";

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
        GitRepository repository = new GitRepository();
        RepositoryInstatiator.setParams(repository, settings);
        repository.initialize();
        return repository;
    }
}
