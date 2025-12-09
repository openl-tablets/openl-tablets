package org.openl.rules.repository.zip;

import java.util.function.Consumer;

import org.openl.rules.repository.RepositoryFactory;
import org.openl.rules.repository.api.Repository;

/**
 * Jar local repository factory.
 *
 * @author Vladyslav Pikus
 */
public class JarRepositoryFactory implements RepositoryFactory {

    private static final String ID = "repo-jar";

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
        JarLocalRepository repository = new JarLocalRepository();
        repository.initialize();
        return repository;
    }

}
