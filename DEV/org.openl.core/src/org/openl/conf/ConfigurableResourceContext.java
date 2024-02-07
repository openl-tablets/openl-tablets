package org.openl.conf;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 */

public class ConfigurableResourceContext implements IConfigurableResourceContext {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurableResourceContext.class);

    private static final String[] DEFAULT_FILESYSTEM_ROOTS = {".", ""};

    private final IOpenLConfiguration config;
    private ClassLoader classLoader;
    private final String[] fileSystemRoots;
    private Map<String, String> properties;

    public ConfigurableResourceContext(ClassLoader classLoader, IOpenLConfiguration config) {
        this(classLoader, DEFAULT_FILESYSTEM_ROOTS, config);
    }

    public ConfigurableResourceContext(ClassLoader classLoader, String[] fileSystemRoots) {
        this(classLoader, fileSystemRoots, null);
    }

    public ConfigurableResourceContext(ClassLoader classLoader, String[] fileSystemRoots, IOpenLConfiguration config) {
        this.classLoader = classLoader;
        this.fileSystemRoots = fileSystemRoots;
        this.config = config;
    }

    public ConfigurableResourceContext(IOpenLConfiguration config) {
        this(Thread.currentThread().getContextClassLoader(), DEFAULT_FILESYSTEM_ROOTS, config);
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    @Override
    public Class<?> findClass(String className) {
        try {
            return getClassLoader().loadClass(className);
        } catch (Exception | LinkageError e) {
            LOG.debug("Failed to load class '{}'.", className, e);
            return null;
        }
    }

    @Override
    public URL findClassPathResource(String url) {
        return getClassLoader().getResource(url);
    }

    @Override
    public File findFileSystemResource(String url) {
        File file = new File(url);

        if (file.isAbsolute() && file.exists()) {
            return file;
        } else {
            for (String fileSystemRoot : fileSystemRoots) {
                file = new File(fileSystemRoot, url);
                if (file.exists()) {
                    return file;
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurableResourceContext#findProperty(java.lang.String )
     */
    @Override
    public String findProperty(String propertyName) {

        String property = null;

        if (properties != null) {
            property = properties.get(propertyName);
        }

        if (property != null) {
            return property;
        }

        return System.getProperty(propertyName);
    }

    @Override
    public ClassLoader getClassLoader() {

        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }

        return classLoader;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurableResourceContext#getConfiguration()
     */
    @Override
    public IOpenLConfiguration getConfiguration() {
        return config;
    }

}
