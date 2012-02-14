package org.openl.rules.ruleservice.client.mapping.openl;

import org.openl.rules.ruleservice.client.mapping.ResultMappingProvider;

public abstract class AbstractJavaResultMapperProvider<K, T> implements ResultMappingProvider<K, T> {
    public abstract T mapToResult(K result, Object... args) throws Exception;
}
