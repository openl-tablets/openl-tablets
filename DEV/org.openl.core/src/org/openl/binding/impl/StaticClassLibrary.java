/*
 * Created on Jun 3, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openl.binding.IOpenLibrary;
import org.openl.binding.exception.AmbiguousMethodException;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;
import org.openl.util.CollectionUtils;

/**
 * @author snshor
 *
 */
public class StaticClassLibrary implements IOpenLibrary {

    Map<String, List<IOpenMethod>> methodNameMap = null;
    private IOpenClass openClass;

    public StaticClassLibrary() {
    }

    public StaticClassLibrary(IOpenClass openClass) {
        this.openClass = openClass;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.IMethodFactory#getMatchingMethod(java.lang.String, java.lang.String,
     * org.openl.types.IOpenClass[])
     */
    @Override
    public IOpenMethod getMethod(String name, IOpenClass[] params) {
        return openClass.getMethod(name, params);
    }

    @Override
    public IOpenField getVar(String name, boolean strictMatch) {
        // This method must return null! See EPBDS-6799.
        return null;
    }

    public void setOpenClass(IOpenClass c) {
        openClass = c;
    }

    @Override
    public Iterable<IOpenMethod> methods(String name) {
        if (methodNameMap == null) {
            synchronized (this) {
                List<IOpenMethod> methods = CollectionUtils.findAll(openClass.getMethods(),
                    new CollectionUtils.Predicate<IOpenMethod>() {
                        @Override
                        public boolean evaluate(IOpenMethod method) {
                            return method.isStatic();
                        }
                    });
                methodNameMap = AOpenClass.buildMethodNameMap(methods);
            }
        }

        List<IOpenMethod> found = methodNameMap.get(name);

        return found == null ? Collections.<IOpenMethod> emptyList() : found;
    }

    @Override
    public IOpenMethod getConstructor(IOpenClass[] params) {
        return null;
    }

    @Override
    public Iterable<IOpenMethod> constructors() {
        return Collections.<IOpenMethod> emptyList();
    }
}
