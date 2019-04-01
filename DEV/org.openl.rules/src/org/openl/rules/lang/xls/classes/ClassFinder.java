package org.openl.rules.lang.xls.classes;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to find a classes in file system.
 *
 * @author NSamatov
 */
public class ClassFinder {
    private final Logger log = LoggerFactory.getLogger(ClassFinder.class);

    private Map<String, ClassLocator> locators = new HashMap<>();

    public ClassFinder() {
        this(Arrays.asList(new LoggingExceptionHandler()));
    }

    public ClassFinder(List<? extends LocatorExceptionHandler> handlers) {
        initDefaultLocators(handlers);
    }

    public void setLocator(String protocol, ClassLocator locator) {
        locators.put(protocol.toLowerCase(), locator);
    }

    /**
     * Scans all classes accessible from the context class loader which belong to the given package.
     *
     * @param packageName The package
     * @return The classes
     */
    public Class<?>[] getClasses(String packageName) {
        return getClasses(packageName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Scans all classes accessible from the given class loader which belong to the given package.
     *
     * @param packageName The package
     * @param classLoader Class Loader
     * @return The classes
     */
    public Class<?>[] getClasses(String packageName, ClassLoader classLoader) {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            return new Class[0];
        }

        Set<Class<?>> classes = new HashSet<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if (protocol != null) {
                ClassLocator locator = locators.get(protocol.toLowerCase());
                if (locator != null) {
                    classes.addAll(locator.getClasses(resource, packageName, classLoader));
                } else {
                    log.warn("A ClassLocator for protocol \"{}\" is not found", protocol);
                }
            }
        }
        return classes.toArray(new Class[classes.size()]);
    }

    private void initDefaultLocators(List<? extends LocatorExceptionHandler> handlers) {
        setLocator("file", new DirectoryClassLocator(handlers));
        setLocator("jar", new JarClassLocator(handlers));
        setLocator("wsjar", new JarClassLocator(handlers)); // Used by IBM WebSphere
        setLocator("zip", new JarClassLocator(handlers)); // Used by BEA WebLogic Server
    }
}
