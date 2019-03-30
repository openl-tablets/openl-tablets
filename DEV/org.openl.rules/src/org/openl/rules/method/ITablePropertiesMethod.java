package org.openl.rules.method;

import java.util.Map;

import org.openl.rules.table.properties.ITableProperties;

public interface ITablePropertiesMethod {
    Map<String, Object> getProperties();

    ITableProperties getMethodProperties();
}
