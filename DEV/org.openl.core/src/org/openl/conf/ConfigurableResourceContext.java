package org.openl.conf;

import java.io.File;
import java.net.URL;

/**
 * @author snshor
 */

public class ConfigurableResourceContext implements IConfigurableResourceContext {

    private static final String[] DEFAULT_FILESYSTEM_ROOTS = {".", ""};

    private final IOpenLConfiguration config;
    private ClassLoader classLoader;
    private final String[] fileSystemRoots;

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
