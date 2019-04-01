package org.openl.types.java;

import java.util.HashMap;
import java.util.Map;

import org.openl.conf.ClassFactory;
import org.openl.types.IOpenClass;
import org.openl.types.ITypeLibrary;

public class JavaLongNameTypeLibrary implements ITypeLibrary {

    // TODO add security ability to block access to system classes. Can be implemented only after java.lang import
    // library is dealt with

    // private String[] blockedClasses;
    // private String[] blockedClassPatterns;

    // public JavaLongNameTypeLibrary(String[] blockedClasses, String[] blockedClassPatterns) {
    // super();
    // this.blockedClasses = blockedClasses;
    // this.blockedClassPatterns = blockedClassPatterns;
    // }

    private Map<String, IOpenClass> foundClasses = new HashMap<>();

    // private Set<String> blocked = new HashSet<String>();

    private ClassLoader loader;

    public JavaLongNameTypeLibrary(ClassLoader classLoader) {
        this.loader = classLoader;
    }

    @Override
    public IOpenClass getType(String typename) {

        IOpenClass ioc = foundClasses.get(typename);
        if (ioc != null) {
            return ioc;
        }

        // if (blocked.contains(typename))
        // throw new ClassNotFoundException(typename + " is blocked by OpenL");

        // for (int i = 0; i < blockedClassPatterns.length; i++) {
        // if (blockedClassPatterns[i].matches(typename))
        // {
        // throw new ClassNotFoundException(typename + " is blocked by OpenL");
        // }
        // }

        try {
            Class<?> c = ClassFactory.forName(typename, loader);
            ioc = JavaOpenClass.getOpenClass(c);
            foundClasses.put(typename, ioc);
            return ioc;
        } catch (Throwable t) {
        }
        return null;
    }
}
