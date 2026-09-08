package org.openl.rules.repository;

import java.util.function.Function;

import org.openl.rules.repository.api.Repository;

/**
 * Creates the repositories of one repository type.
 *
 * <p>A factory is declared on the class path as a service and is created once. It is then asked from
 * any thread and for any number of repositories, so an implementation keeps no state of its own.
 */
public interface RepositoryFactory {

    boolean accept(String factoryID);

    String getRefID();

    Repository create(Function<String, String> settings);
}
