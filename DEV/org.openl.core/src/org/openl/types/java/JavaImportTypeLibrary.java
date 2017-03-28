/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.binding.exception.AmbiguousTypeException;
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

    private HashMap<String, IOpenClass> aliases = new HashMap<String, IOpenClass>();

    private Set<String> notFound = new HashSet<String>();

    private List<String> importPackages = new ArrayList<String>();

    private ClassLoader loader;

    public JavaImportTypeLibrary(List<String> importClasses, List<String> importPackages, ClassLoader loader) {
        this.loader = loader;
        this.importPackages = importPackages;
        if (importClasses != null) {
            for (String importClass : importClasses) {
                int index = importClass.lastIndexOf('.');
                String alias = importClass.substring(index + 1);

                Class<?> c = ClassFactory.forName(importClass, loader);
                aliases.put(alias, JavaOpenClass.getOpenClass(c));

            }
        }
    }

    protected ClassLoader getClassLoader() {
        return loader;
    }

    public synchronized IOpenClass getType(String typename) throws AmbiguousTypeException {

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
                // Type is found but can't be loaded because of absent dependent class.
                String noClassMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                String message = String.format("Type '%s' can't be loaded because of absent type '%s'.", name, noClassMessage);
                throw RuntimeExceptionWrapper.wrap(message, e);
            } catch (UnsupportedClassVersionError e) {
                // Type is found but it's compiled using newer version of JDK
                String message = String.format("Can't load the class \"%s\" compiled using newer version of JDK than current JRE (%s)", name, System.getProperty("java.version"));
                throw RuntimeExceptionWrapper.wrap(message, e);
            } catch (Throwable t) {
                Log.error("Error loading class: " + name, t);
                throw RuntimeExceptionWrapper.wrap(t);
            }
        }
        notFound.add(typename);
        return null;

    }
}
