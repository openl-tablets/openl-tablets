package org.openl.rules.repository;

import java.util.function.Consumer;

import org.openl.rules.repository.api.Repository;

public interface RepositoryFactory {

    boolean accept(String factoryID);

    String getRefID();

    Repository create(Consumer<Repository> initParamsCallback);
}
