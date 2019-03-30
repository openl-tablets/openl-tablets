/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.conf.ClassFactory;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author snshor
 *
 */
public class JavaImportTypeLibrary implements ITypeLibrary {

    private Map<String, IOpenClass> aliases = new HashMap<>();

    private Set<String> notFound = new HashSet<>();

    private String[] importPackages;

    private ClassLoader loader;

    public JavaImportTypeLibrary(String[] importPackages, String[] importClasses, ClassLoader loader) {
        this.loader = loader;
        if (importPackages == null) {
            this.importPackages = new String[] {};
        } else {
            this.importPackages = importPackages;
        }
        if (importClasses != null) {
            for (String importClass : importClasses) {
                int index = importClass.lastIndexOf('.');
                String alias = importClass.substring(index + 1);
                try {
                    Class<?> c = ClassFactory.forName(importClass, loader);
                    aliases.put(alias, JavaOpenClass.getOpenClass(c));
                } catch (Exception e) {
                    // This never happens. Classes must be validated before.
                }
            }
        }
    }

    protected ClassLoader getClassLoader() {
        return loader;
    }

    @Override
    public synchronized IOpenClass getType(String typename) {

        IOpenClass oc = aliases.get(typename);
        if (oc != null) {
            return oc;
        }
        if (notFound.contains(typename)) {
            return null;
        }
        // TODO use imports
        for (String singleImport : importPackages) {
            String name = singleImport + "." + typename;
            try {
                Class<?> c = getClassLoader().loadClass(name);
                oc = JavaOpenClass.getOpenClass(c);
                aliases.put(typename, oc);
                return oc;
            } catch (ClassNotFoundException ignored) {
                // Type isn't found in the package. Search in another.
            } catch (NoClassDefFoundError e) {
                if (e.getCause() instanceof ClassNotFoundException) {
                    // Type is found but can't be loaded because of absent dependent class.
                    String noClassMessage = e.getCause().getMessage();
                    String message = String
                        .format("Can't load type '%s' because of absent type '%s'.", name, noClassMessage);
                    throw RuntimeExceptionWrapper.wrap(message, e);
                }
                // NoClassDefFoundError can also be thrown in these cases:
                // 1. Class was compiled in one package but it was moved manually to another package in file system
                // without changing package in class binary
                // 2. Class was compiled with one name but was manually renamed in file system to another name
                // 3. If File System is case insensitive and we are trying to find the class org.work.address but exists
                // the class org.work.Address
                // In all these cases NoClassDefFoundError will be thrown instead of ClassNotFoundException and message
                // will be like:
                // java.lang.NoClassDefFoundError: org/work/address (wrong name: org/work/Address)
                // We just skip such classes and continue searching them in another packages.
            } catch (UnsupportedClassVersionError e) {
                // Type is found but it's compiled using newer version of JDK
                String message = String.format(
                    "Can't load the class '%s' that was compiled using newer version of JDK than current JRE (%s)",
                    name,
                    System.getProperty("java.version"));
                throw RuntimeExceptionWrapper.wrap(message, e);
            } catch (Throwable t) {
                Log.error("Can't load class: " + name, t);
                throw RuntimeExceptionWrapper.wrap(t);
            }
        }
        notFound.add(typename);
        return null;

    }
}
