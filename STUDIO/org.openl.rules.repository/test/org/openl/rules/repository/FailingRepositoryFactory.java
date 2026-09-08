package org.openl.rules.repository;

import java.util.function.Function;

import org.openl.rules.repository.api.Repository;

/**
 * A factory that cannot be created.
 *
 * <p>It is declared alongside the working factories so that leaving it out of the lookup is exercised:
 * a factory whose class is on the class path without everything it needs behaves this way.
 */
public class FailingRepositoryFactory implements RepositoryFactory {

    public FailingRepositoryFactory() {
        throw new IllegalStateException("This factory cannot be created.");
    }

    @Override
    public boolean accept(String factoryID) {
        return false;
    }

    @Override
    public String getRefID() {
        return "repo-failing-test-only";
    }

    @Override
    public Repository create(Function<String, String> settings) {
        throw new UnsupportedOperationException("This factory accepts no repository type.");
    }
}
