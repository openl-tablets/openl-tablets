/*
 * Created on Jun 4, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.io.File;
import java.net.URL;
import java.util.Properties;

// import org.openl.util.Log;

/**
 * @author snshor
 *
 */

public class ConfigurableResourceContext implements IConfigurableResourceContext {
    static final String[] DEFAULT_FILESYSTEM_ROOTS = { ".", "" };

    IOpenLConfiguration conf;

    ClassLoader classLoader = null;

    String[] fileSystemRoots = null;

    Properties properties = null;

    public ConfigurableResourceContext(ClassLoader cl, IOpenLConfiguration conf) {
        this(cl, DEFAULT_FILESYSTEM_ROOTS, conf);

    }

    public ConfigurableResourceContext(ClassLoader cl, String[] fileSystemRoots) {
        this(cl, fileSystemRoots, null);
    }

    public ConfigurableResourceContext(ClassLoader cl, String[] fileSystemRoots, IOpenLConfiguration conf) {
        classLoader = cl;
        this.fileSystemRoots = fileSystemRoots;
        this.conf = conf;
    }

    public ConfigurableResourceContext(IOpenLConfiguration conf) {
        this(Thread.currentThread().getContextClassLoader(), DEFAULT_FILESYSTEM_ROOTS, conf);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurableResourceContext#findClass(java.lang.String)
     */
    public Class<?> findClass(String className) {
        try {
            return getClassLoader().loadClass(className);
        } catch (Throwable t) {
            // Log.debug("", t);
            return null;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurableResourceContext#findURLResource(java.lang.String)
     */
    public URL findClassPathResource(String url) {
        return getClassLoader().getResource(url);
    }

    public File findFileSystemResource(String url) {
        File f = new File(url);

        if (f.isAbsolute()) {
            if (f.exists()) {
                return f;
            }
        } else {
            for (int i = 0; i < fileSystemRoots.length; i++) {
                f = new File(fileSystemRoots[i], url);
                if (f.exists()) {
                    return f;
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.conf.IConfigurableResourceContext#findProperty(java.lang.String)
     */
    public String findProperty(String propertyName) {
        String property = properties == null ? null : properties.getProperty(propertyName);
        if (property != null) {
            return property;
        }

        return System.getProperty(propertyName);
    }

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
    public IOpenLConfiguration getConfiguration() {
        return conf;
    }

    /**
     * @return
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * @param properties
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }

}
