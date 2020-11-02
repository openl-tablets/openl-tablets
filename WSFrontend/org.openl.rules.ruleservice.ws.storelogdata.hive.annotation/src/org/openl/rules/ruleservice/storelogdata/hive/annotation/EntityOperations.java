package org.openl.rules.ruleservice.storelogdata.hive.annotation;

import java.sql.Connection;
import java.util.concurrent.CompletionStage;


public interface EntityOperations<T, E> {
    T buildDao(Connection connection);

    CompletionStage<Void> insert(T dao, E entity);
}

