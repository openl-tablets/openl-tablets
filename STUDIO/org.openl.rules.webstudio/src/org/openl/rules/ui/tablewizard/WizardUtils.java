package org.openl.rules.ui.tablewizard;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openl.base.INamedThing;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Aliaksandr Antonik.
 */
public final class WizardUtils {

    private static final Logger LOG = LoggerFactory.getLogger(WizardUtils.class);

    private WizardUtils() {
    }

    private static final List<String> predefinedTypes;
    static {
        ArrayList<String> types = new ArrayList<>();

        // The most popular
        types.add("String");
        types.add("Double");
        types.add("Integer");
        types.add("Boolean");
        types.add("Date");

        types.add("BigInteger");
        types.add("BigDecimal");

        types.add("IntRange");
        types.add("DoubleRange");

        types.add("Long");
        types.add("Float");
        types.add("Short");
        types.add("Character");

        // Less popular
        types.add("byte");
        types.add("short");
        types.add("int");
        types.add("long");
        types.add("float");
        types.add("double");
        types.add("boolean");
        types.add("char");

        predefinedTypes = Collections.unmodifiableList(types);
    }
    static List<String> predefinedTypes() {
        return predefinedTypes;
    }

    static List<String> declaredDatatypes() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DatatypeOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> declaredAliases() {
        return getProjectOpenClass().getTypes()
            .stream()
            .filter(t -> t instanceof DomainOpenClass)
            .map(IOpenClass::getName)
            .sorted()
            .collect(Collectors.toList());
    }

    static List<String> importedClasses() {
        return getImportedClasses().stream()
            .filter(t -> t instanceof JavaOpenClass)
            .map(v -> v.getDisplayName(INamedThing.SHORT))
            .sorted()
            .collect(Collectors.toList());
    }

    public static IOpenClass getProjectOpenClass() {
        return WebStudioUtils.getProjectModel().getCompiledOpenClass().getOpenClassWithErrors();
    }

    public static TableSyntaxNode[] getTableSyntaxNodes() {
        return WebStudioUtils.getProjectModel().getTableSyntaxNodes();
    }

    /**
     * Get imported classes for current project
     *
     * @return collection, containing an imported classes
     */
    public static Collection<IOpenClass> getImportedClasses() {
        Set<IOpenClass> classes = new TreeSet<>(
            (o1, o2) -> o1.getDisplayName(INamedThing.SHORT).compareToIgnoreCase(o2.getDisplayName(INamedThing.SHORT)));

        for (String packageName : WebStudioUtils.getProjectModel().getXlsModuleNode().getImports()) {
            if ("org.openl.rules.enumeration".equals(packageName)) {
                // This package is added automatically in XlsLoader.addInnerImports() for inner usage, not for user.
                continue;
            }
            ClassLoader classLoader = WebStudioUtils.getProjectModel().getCompiledOpenClass().getClassLoader();
            for (Class<?> type : getClasses(packageName, classLoader)) {
                IOpenClass openType;
                try {
                    openType = JavaOpenClass.getOpenClass(type);
                } catch (Exception e) {
                    // For example NoClassDefFoundError when the class for some of the fields is absent.
                    final Logger log = LoggerFactory.getLogger(WizardUtils.class);
                    log.debug("Cannot load the class, skip it because it's not valid. Cause: {}", e.getMessage(), e);
                    continue;
                }
                if (!isValid(openType)) {
                    continue;
                }

                classes.add(openType);
            }
        }

        return classes;
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
            LOG.debug(e.getMessage(), e);
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
                    case "wsjar": // Used by IBM WebSphere
                        loadFromJar(classes, packageName, classLoader, resource);
                        break;
                    default:
                        LOG.warn("A ClassLocator for protocol '{}' is not found.", protocol);
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
            LOG.error(e.getMessage(), e);
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
                                LOG.debug(e.getMessage(), e);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
            }
        }
    }

    private static void loadFromDirectory(Set<Class<?>> classes, String packageName, ClassLoader classLoader, URL pathURL) {
        File directory;

        try {
            directory = new File(pathURL.toURI());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
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
                            LOG.debug(e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

}
