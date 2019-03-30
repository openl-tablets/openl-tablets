package org.openl.rules.lang.xls.classes;

import java.net.URL;
import java.util.Collection;

/**
 * Is used to locate a classes in a given URL path
 * 
 * @author NSamatov
 */
public interface ClassLocator {
    /**
     * Find all classes in a given path. If a class cannot be loaded, it is skipped (in our case we don't need such
     * classes).
     * 
     * @param pathURL The path where classes is searched
     * @param packageName The package name for classes found inside the path
     * @param classLoader a ClassLoader that is used to load a classes
     * @return Found classes
     */
    Collection<Class<?>> getClasses(URL pathURL, String packageName, ClassLoader classLoader);
}
