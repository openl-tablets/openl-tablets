package org.openl.rules.lang.xls.classes;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class is used to find a classes in file system.
 * 
 * @author NSamatov
 */
public class ClassFinder {
    private final Log log = LogFactory.getLog(ClassFinder.class);

    private Map<String, ClassLocator> locators = new HashMap<String, ClassLocator>();

    public ClassFinder() {
        this(Arrays.asList(new LoggingExceptionHandler(), new OpenLMessageExceptionHandler()));
    }

    public ClassFinder(List<? extends LocatorExceptionHandler> handlers) {
        initDefaultLocators(handlers);
    }

    public void setLocator(String protocol, ClassLocator locator) {
        locators.put(protocol.toLowerCase(), locator);
    }

    /**
     * Scans all classes accessible from the context class loader which belong
     * to the given package.
     * 
     * @param packageName The package
     * @return The classes
     */
    public Class<?>[] getClasses(String packageName) {
        return getClasses(packageName, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Scans all classes accessible from the given class loader which belong to
     * the given package.
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
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return new Class[0];
        }

        Set<Class<?>> classes = new HashSet<Class<?>>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if (protocol != null) {
                ClassLocator locator = locators.get(protocol.toLowerCase());
                if (locator != null) {
                    classes.addAll(locator.getClasses(resource, packageName, classLoader));
                } else {
                    if (log.isWarnEnabled()) {
                        String message = String.format("A ClassLocator for protocol \"%s\" not found", protocol);
                        log.warn(message);
                    }
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
