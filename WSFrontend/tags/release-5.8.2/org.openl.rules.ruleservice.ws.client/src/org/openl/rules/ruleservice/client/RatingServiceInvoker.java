package org.openl.rules.ruleservice.client;

public interface RatingServiceInvoker<T> {
    public T invoke(Object... args) throws Exception;
}
