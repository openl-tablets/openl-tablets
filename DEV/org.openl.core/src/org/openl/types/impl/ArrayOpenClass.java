/*
 * Created on Jun 24, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.util.HashMap;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class ArrayOpenClass extends AOpenClass {

    protected IOpenClass componentClass;
    protected HashMap<String, IOpenField> fieldMap;
    protected IOpenIndex index;

    public ArrayOpenClass(IOpenClass componentClass, IOpenField lengthOpenField) {
        this.componentClass = componentClass;
        fieldMap = new HashMap<String, IOpenField>(1);
        fieldMap.put(lengthOpenField.getName(), lengthOpenField);
    }

    @Override
    protected Map<String, IOpenField> fieldMap() {
        return fieldMap;
    }

    @Override
    public IOpenClass getComponentClass() {
        return componentClass;
    }

    public String getDisplayName(int mode) {
        return componentClass.getDisplayName(mode) + "[]";
    }

    public Class<?> getInstanceClass() {
        if (componentClass.getInstanceClass() != null) {
            return JavaOpenClass.makeArrayClass(componentClass.getInstanceClass());
        } else {
            return null;
        }
    }

    @Override
    public boolean isAssignableFrom(IOpenClass ioc) {
        return getInstanceClass().isAssignableFrom(ioc.getInstanceClass());
    }
    
    @Override
    public boolean isAssignableFrom(Class<?> c) {
        return getInstanceClass().isAssignableFrom(c);
    }
    
    @Override
    public boolean isInstance(Object instance) {
        return getInstanceClass().isInstance(instance);
    }
    
    public String getName() {
        return componentClass.getName() + "[]";
    }

    @Override
    public String getJavaName() {
        String componentName = componentClass.getJavaName();
        if (componentName.charAt(0) == '[') {
            return '[' + componentName;
        } else {
            return "[L" + componentName + ';';
        }
    }

    @Override
    public boolean isArray() {
        return true;
    }

}
