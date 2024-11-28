package org.openl.rules.repository;

import java.util.function.Function;

import org.openl.rules.repository.api.Repository;

public interface RepositoryFactory {

    boolean accept(String factoryID);

    String getRefID();

    Repository create(Function<String, String> settings);
}
