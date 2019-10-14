package org.openl.rules.ruleservice.storelogdata.cassandra.annotation;

import java.util.concurrent.CompletionStage;

import com.datastax.oss.driver.api.core.CqlSession;

public interface EntityOperations<T, E> {
    T buildDao(CqlSession session) throws DaoCreationException;

    CompletionStage<Void> insert(T dao, E entity);
}
