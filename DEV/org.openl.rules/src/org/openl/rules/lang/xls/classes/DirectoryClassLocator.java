package org.openl.rules.lang.xls.classes;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Finds a classes in a given directory
 *
 * @author NSamatov
 */
public class DirectoryClassLocator implements ClassLocator {
    private final List<LocatorExceptionHandler> handlers;

    public DirectoryClassLocator() {
        this(new ArrayList<LocatorExceptionHandler>());
    }

    public DirectoryClassLocator(List<? extends LocatorExceptionHandler> handlers) {
        this.handlers = new ArrayList<>(handlers);
    }

    /**
     * Add exception handler
     *
     * @param handler exception handler
     */
    public void addExceptionHandler(LocatorExceptionHandler handler) {
        handlers.add(handler);
    }

    /**
     * Find all classes in a given directory. If a class cannot be loaded, it is skipped (in our case we don't need such
     * classes).
     *
     * @param pathURL path to the directory
     * @param packageName The package name for classes found inside the directory
     * @param classLoader a ClassLoader that is used to load a classes
     * @return Found classes
     */
    @Override
    public Collection<Class<?>> getClasses(URL pathURL, String packageName, ClassLoader classLoader) {
        File directory;

        try {
            directory = new File(pathURL.toURI());
        } catch (Exception e) {
            for (LocatorExceptionHandler handler : handlers) {
                handler.handleURLParseException(e);
            }
            return Collections.emptySet();
        }

        Set<Class<?>> classes = new HashSet<>();
        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();

        for (File file : files) {
            String fileName = file.getName();
            if (!file.isDirectory()) {
                String suffix = ".class";
                if (fileName.endsWith(suffix) && !fileName.contains("$")) {
                    try {
                        String className = fileName.substring(0, fileName.length() - suffix.length());
                        String fullClassName = packageName + '.' + className;
                        Class<?> type = Class.forName(fullClassName, true, classLoader);
                        classes.add(type);
                    } catch (Throwable t) {
                        for (LocatorExceptionHandler handler : handlers) {
                            handler.handleClassInstatiateException(t);
                        }
                    }
                }
            }
        }
        return classes;
    }

}
