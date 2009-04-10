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
import org.openl.types.IOpenSchema;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public abstract class ArrayOpenClass extends AOpenClass {

    protected IOpenClass componentClass;
    protected HashMap<String, IOpenField> fieldMap;
    protected IOpenIndex index;

    /**
     * @param schema
     */
    public ArrayOpenClass(IOpenSchema schema, IOpenClass componentClass, IOpenField lengthOpenField) {
        super(schema);
        this.componentClass = componentClass;
        fieldMap = new HashMap<String, IOpenField>(1);
        fieldMap.put(lengthOpenField.getName(), lengthOpenField);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.AOpenClass#fieldMap()
     */
    @Override
    protected Map<String, IOpenField> fieldMap() {
        return fieldMap;
    }

    /**
     * @return
     */
    public IOpenClass getComponentClass() {
        return componentClass;
    }

    public String getDisplayName(int mode) {
        return componentClass.getDisplayName(mode) + "[]";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenClass#getInstanceClass()
     */
    public Class<?> getInstanceClass() {
        return JavaOpenClass.makeArrayClass(componentClass.getInstanceClass());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return componentClass.getName() + "[]";
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.AOpenClass#methodMap()
     */
    @Override
    protected Map<MethodKey, IOpenMethod> methodMap() {
        return null;
    }

}
