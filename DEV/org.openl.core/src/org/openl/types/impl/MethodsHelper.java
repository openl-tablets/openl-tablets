package org.openl.types.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class MethodsHelper {
    
    private MethodsHelper() {}

    public static IOpenMethod[] getMethods(String name, Collection<IOpenMethod> methods) {
        Collection<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (IOpenMethod m : methods) {
            if (name.equals(m.getName())) {
                list.add(m);
            }
        }

        if (list.isEmpty()) {
            throw new MethodNotFoundException(null, name, IOpenClass.EMPTY);
        }
        
        return list.toArray(new IOpenMethod[0]);
    }

    public static IOpenMethod getSingleMethod(String name, Collection<IOpenMethod> methods) {
        List<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (IOpenMethod m : methods) {
            if (m.getName().equals(name)) {
                list.add(m);
            }
        }

        if (list.isEmpty()) {
            throw new MethodNotFoundException(null, name, IOpenClass.EMPTY);
        }

        if (list.size() > 1) {
            throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
        }

        return list.get(0);
    }

}
