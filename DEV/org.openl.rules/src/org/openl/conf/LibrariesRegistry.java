package org.openl.conf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openl.binding.ICastFactory;
import org.openl.binding.exception.AmbiguousFieldException;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.binding.impl.method.MethodSearch;
import org.openl.types.IMethodCaller;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.MethodKey;
import org.openl.types.java.JavaOpenClass;

/**
 * @author Yury Molchan
 */
public class LibrariesRegistry {

    private final HashMap<String, List<IOpenMethod>> methods = new HashMap<>();
    private final HashMap<String, List<IOpenField>> constants = new HashMap<>();

    public void addJavalib(Class<?> className) {
        JavaOpenClass openClass = JavaOpenClass.getOpenClass(className);
        for (var method : openClass.getDeclaredMethods()) {
            if (method.isStatic()) {
                methods.computeIfAbsent(method.getName(), x -> new ArrayList<>()).add(method);
            }
        }
        for (var field : openClass.getDeclaredFields()) {
            if (field.isStatic()) {
                constants.computeIfAbsent(field.getName(), x -> new ArrayList<>()).add(field);
            }
        }
    }

    public IMethodCaller getMethodCaller(String name, IOpenClass[] params, ICastFactory casts, boolean allowMultiCalls, LibrariesRegistry parent) throws AmbiguousMethodException {
        if (parent == null) {
            return MethodSearch.findMethod(name, params, casts, methods.get(name), allowMultiCalls);
        }

        HashMap<MethodKey, ArrayList<IOpenMethod>> uniques = new HashMap<>();
        // Shadowing
        for (var method : parent.methods.getOrDefault(name, Collections.emptyList())) {
            uniques.computeIfAbsent(new MethodKey(method), k -> new ArrayList<>()).add(method);
        }

        HashMap<MethodKey, ArrayList<IOpenMethod>> current = new HashMap<>();
        for (var method : methods.getOrDefault(name, Collections.emptyList())) {
            current.computeIfAbsent(new MethodKey(method), k -> new ArrayList<>()).add(method);
        }

        // Overriding of the parent methods if they exist.
        uniques.putAll(current);

        var openMethods = new ArrayList<IOpenMethod>();
        for (var m : uniques.values()) {
            openMethods.addAll(m);
        }

        return MethodSearch.findMethod(name, params, casts, openMethods, allowMultiCalls);
    }

    public IOpenField getField(String name) throws AmbiguousFieldException {
        if (name.equals("class")) {
            return null;
        }
        var fields = constants.get(name);
        if (fields == null || fields.isEmpty()) {
            return null;
        } else if (fields.size() == 1) {
            return fields.getFirst();
        }
        throw new AmbiguousFieldException(name, fields);
    }
}
