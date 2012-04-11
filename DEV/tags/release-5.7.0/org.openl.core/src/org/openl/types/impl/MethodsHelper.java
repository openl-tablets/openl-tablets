package org.openl.types.impl;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.exception.MethodNotFoundException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public class MethodsHelper {
    
    private MethodsHelper() {}

    @Deprecated
    public static IOpenMethod[] getMethods(String name, Iterator<IOpenMethod> methods) {
        ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (; methods.hasNext();) {
            IOpenMethod m = methods.next();
            if (m.getName().equals(name)) {
                list.add(m);
            }
        }
    
        return list.toArray(new IOpenMethod[0]);
    }

    public static IOpenMethod getSingleMethod(String name, Iterator<IOpenMethod> methods) {
        ArrayList<IOpenMethod> list = new ArrayList<IOpenMethod>();
        for (; methods.hasNext();) {
            IOpenMethod m = methods.next();
            if (m.getName().equals(name)) {
                list.add(m);
            }
        }
    
        if (list.size() == 0) {
            throw new MethodNotFoundException(null, name, IOpenClass.EMPTY);
        }
    
        if (list.size() > 1) {
            throw new AmbiguousMethodException(name, IOpenClass.EMPTY, list);
        }
    
        return list.get(0);
    };

}
