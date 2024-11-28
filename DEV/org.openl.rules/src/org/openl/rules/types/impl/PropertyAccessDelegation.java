package org.openl.rules.types.impl;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.properties.ITableProperties;

public abstract class PropertyAccessDelegation<T, C> {

    protected abstract C getContextValue(IRulesRuntimeContext context);

    protected abstract T getPropertyValue(ITableProperties properties);

}
