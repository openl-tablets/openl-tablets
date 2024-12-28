package org.openl.conf;

/**
 * @author snshor
 */

public class ConfigurableResourceContext implements IConfigurableResourceContext {

    private ClassLoader classLoader;

    public ConfigurableResourceContext(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public ClassLoader getClassLoader() {

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        return classLoader;
    }

}
