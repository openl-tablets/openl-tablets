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
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class ArrayOpenClass extends AOpenClass {

    protected IOpenClass componentClass;
    protected HashMap<String, IOpenField> fieldMap;
    protected Map<MethodKey, IOpenMethod> methodMap = new HashMap<MethodKey, IOpenMethod>();
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

    public String getName() {
        return componentClass.getName() + "[]";
    }

    @Override
    protected Map<MethodKey, IOpenMethod> methodMap() {
        return methodMap;
    }
    
    @Override
    public boolean isArray() {            
        return true;
    }

}
