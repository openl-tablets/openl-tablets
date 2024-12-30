package org.openl.conf;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openl.binding.IOpenLibrary;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.impl.StaticClassLibrary;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author Yury Molchan
 */
public class LibrariesRegistry {

    private final ArrayList<IOpenLibrary> factories = new ArrayList<>();

    public void addJavalib(Class<?> className) {
        factories.add(new StaticClassLibrary(JavaOpenClass.getOpenClass(className)));
    }

    public IOpenField getField(String name, boolean strictMatch) throws AmbiguousFieldException {
        List<IOpenField> fields = new ArrayList<>();
        for (var factory : factories) {
            IOpenField field = factory.getVar(name, strictMatch);
            if (field != null) {
                fields.add(field);
            }
        }
        if (fields.isEmpty()) {
            return null;
        } else if (fields.size() == 1) {
            return fields.iterator().next();
        }
        throw new AmbiguousFieldException(name, fields);
    }

    public IOpenMethod[] getMethods(String name) {
        List<IOpenMethod> methods = new LinkedList<>();
        for (var factory : factories) {
            Iterable<IOpenMethod> itr = factory.methods(name);
            for (IOpenMethod method : itr) {
                methods.add(method);
            }
        }

        return methods.toArray(IOpenMethod.EMPTY_ARRAY);
    }
}
