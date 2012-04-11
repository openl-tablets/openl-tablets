/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.openl.binding.AmbiguousTypeException;
import org.openl.conf.ClassFactory;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

/**
 * @author snshor
 *
 */
public class JavaImportTypeLibrary implements ITypeLibrary {

    HashMap<String, IOpenClass> aliases = new HashMap<String, IOpenClass>();

    Set<String> notFound = new HashSet<String>();

    String[] importPackages;

    ClassLoader loader;

    public JavaImportTypeLibrary(String[] importClasses, String[] importPackages, ClassLoader loader) {
        this.loader = loader;
        this.importPackages = importPackages;
        if (importClasses != null) {
            for (int i = 0; i < importClasses.length; i++) {
                int index = importClasses[i].lastIndexOf('.');
                String alias = importClasses[i].substring(index + 1);

                Class<?> c = ClassFactory.forName(importClasses[i], loader);
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
        for (int i = 0; i < importPackages.length; i++) {
            try {
                Class<?> c = ClassFactory.forName(importPackages[i] + "." + typename, getClassLoader());
                oc = JavaOpenClass.getOpenClass(c);
                aliases.put(typename, oc);
                return oc;
            } catch (Throwable t) {
            }
        }

        notFound.add(typename);
        return null;

    }

    public Iterator<String> typeNames() {
        // TODO Auto-generated method stub
        return null;
    }

}
