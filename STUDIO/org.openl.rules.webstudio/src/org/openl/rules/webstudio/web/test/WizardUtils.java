package org.openl.rules.webstudio.web.test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.extern.slf4j.Slf4j;

import org.openl.base.INamedThing;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author Aliaksandr Antonik.
 */
@Slf4j
final class WizardUtils {

    private WizardUtils() {
    }

    public static IOpenClass getProjectOpenClass() {
        return WebStudioUtils.getProjectModel().getCompiledOpenClass().getOpenClassWithErrors();
    }

    /**
     * Get imported classes for current project
     *
     * @return collection, containing an imported classes
     */
    public static Collection<IOpenClass> getImportedClasses() {
        var projectModel = WebStudioUtils.getProjectModel();
        var classLoader = projectModel.getCompiledOpenClass().getClassLoader();
        Set<IOpenClass> classes = new TreeSet<>(Comparator
                .comparing(type -> type.getDisplayName(INamedThing.SHORT), String.CASE_INSENSITIVE_ORDER));

        for (String packageName : projectModel.getXlsModuleNode().getImports()) {
            // org.openl.rules.enumeration is added automatically in XlsLoader.addInnerImports() for inner usage,
            // not for the user.
            if (!"org.openl.rules.enumeration".equals(packageName)) {
                getClasses(packageName, classLoader).stream()
                        .map(WizardUtils::asOpenClass)
                        .filter(Objects::nonNull)
                        .filter(WizardUtils::isValid)
                        .forEach(classes::add);
            }
        }

        return classes;
    }

    /**
     * The OpenL view of a class, or {@code null} when it cannot be loaded.
     *
     * <p>A class whose field names an absent class cannot be described, and is left out rather than failing the
     * whole list.
     */
    private static IOpenClass asOpenClass(Class<?> type) {
        try {
            return JavaOpenClass.getOpenClass(type);
        } catch (Exception e) {
            log.debug("Cannot load the class, skip it because it's not valid. Cause: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if type is valid (for example, it can be used in a DataType tables, Data tables etc)
     *
     * @param openType checked type
     * @return true if class is valid.
     */
    private static boolean isValid(IOpenClass openType) {
        Class<?> instanceClass = openType.getInstanceClass();

        int modifiers = instanceClass.getModifiers();
        if (!Modifier.isPublic(modifiers) || Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)) {
            return false;
        }

        // Every field has a "class" field. We skip a classes that does not
        // have any other field.
        return !openType.getFields().isEmpty();

    }


    /**
     * Scans all classes accessible from the given class loader which belong to the given package.
     *
     * @param packageName The package
     * @param classLoader Class Loader
     * @return The classes
     */
    static Set<Class<?>> getClasses(String packageName, ClassLoader classLoader) {
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources;
        try {
            resources = classLoader.getResources(path);
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            return Collections.emptySet();
        }

        Set<Class<?>> classes = new HashSet<>();

        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            String protocol = resource.getProtocol();

            if (protocol != null) {
                switch (protocol.toLowerCase()) {
                    case "file":
                        loadFromDirectory(classes, packageName, classLoader, resource);
                        break;
                    case "jar":
                    case "zip": // Used by BEA WebLogic Server
                        loadFromJar(classes, packageName, classLoader, resource);
                        break;
                    default:
                        log.warn("A ClassLocator for protocol '{}' is not found.", protocol);
                }
            }
        }
        return classes;
    }

    private static void loadFromJar(Set<Class<?>> classes, String packageName, ClassLoader classLoader, URL pathURL) {
        String jarPath = pathURL.getFile().split("!")[0];
        URL jar;
        try {
            jar = new URL(jarPath);
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
            return;
        }

        ZipInputStream zip = null;
        try {
            zip = new ZipInputStream(jar.openStream());
            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().endsWith(".class")) {
                    String fullClassName = entry.getName().replace(".class", "").replace('/', '.');
                    if (fullClassName.startsWith(packageName)) {
                        String className = fullClassName.substring(packageName.length() + 1);
                        if (!className.contains(".") && !className.contains("$")) {
                            try {
                                classes.add(Class.forName(fullClassName, true, classLoader));
                            } catch (Exception | LinkageError e) {
                                log.debug(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    private static void loadFromDirectory(Set<Class<?>> classes, String packageName, ClassLoader classLoader, URL pathURL) {
        File directory;

        try {
            directory = new File(pathURL.toURI());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return;
        }

        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
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
                        } catch (Exception | LinkageError e) {
                            log.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

}
