package org.openl.rules.repository;

import org.openl.rules.repository.api.Repository;

import java.util.function.Function;

public interface RepositoryFactory {

    boolean accept(String factoryID);

    String getRefID();

    Repository create(Function<String, String> settings);
}
