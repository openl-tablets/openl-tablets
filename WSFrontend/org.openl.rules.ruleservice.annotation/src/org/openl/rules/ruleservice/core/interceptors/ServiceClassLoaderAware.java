package org.openl.rules.ruleservice.core.interceptors;

/**
 * This interface is designed to inject @{@link org.openl.classloader.OpenLClassLoader} related to compiled service to
 * ruleservice interceptors.
 */
public interface ServiceClassLoaderAware {
    void setServiceClassLoader(ClassLoader classLoader);
}
