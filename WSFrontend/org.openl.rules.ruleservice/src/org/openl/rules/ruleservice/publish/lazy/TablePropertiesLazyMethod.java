package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IOpenMethod;

public abstract class TablePropertiesLazyMethod extends LazyMethod implements ITablePropertiesMethod {

    public TablePropertiesLazyMethod(String methodName,
            Class<?>[] argTypes,
            IOpenMethod original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(methodName, argTypes, original, dependencyManager, classLoader, executionMode, externalParameters);
    }

    @Override
    public Map<String, Object> getProperties() {
        if (getOriginal() instanceof ITablePropertiesMethod) {
            return ((ITablePropertiesMethod) getOriginal()).getProperties();
        }
        throw new IllegalStateException("Original method must be the instance of ITablePropertiesMethod.");
    }

    @Override
    public ITableProperties getMethodProperties() {
        if (getOriginal() instanceof ITablePropertiesMethod) {
            return ((ITablePropertiesMethod) getOriginal()).getMethodProperties();
        }
        throw new IllegalStateException("Original method must be the instance of ITablePropertiesMethod.");
    }
}
