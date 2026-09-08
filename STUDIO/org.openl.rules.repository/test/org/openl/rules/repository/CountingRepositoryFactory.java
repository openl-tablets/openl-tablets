package org.openl.rules.repository;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.openl.rules.repository.api.Repository;

/**
 * Counts how many times the repository factories are looked up on the class path.
 *
 * <p>A lookup builds every factory it finds, so the number of times this one was built is the number of
 * lookups. It accepts no repository type, so it takes part in nothing else.
 */
public class CountingRepositoryFactory implements RepositoryFactory {

    static final AtomicInteger BUILT = new AtomicInteger();

    public CountingRepositoryFactory() {
        BUILT.incrementAndGet();
    }

    @Override
    public boolean accept(String factoryID) {
        return false;
    }

    @Override
    public String getRefID() {
        return "repo-counting-test-only";
    }

    @Override
    public Repository create(Function<String, String> settings) {
        throw new UnsupportedOperationException("This factory accepts no repository type.");
    }
}
