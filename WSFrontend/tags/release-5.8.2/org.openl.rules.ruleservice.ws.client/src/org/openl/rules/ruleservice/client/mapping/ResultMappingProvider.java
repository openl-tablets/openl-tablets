package org.openl.rules.ruleservice.client.mapping;

public interface ResultMappingProvider<K, T> {
    public T mapToResult(K result, Object... args) throws Exception;
}
