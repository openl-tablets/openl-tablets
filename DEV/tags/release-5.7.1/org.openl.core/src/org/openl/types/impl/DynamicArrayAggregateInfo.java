/*
 * Created on Mar 9, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.Iterator;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DynamicArrayAggregateInfo extends AAggregateInfo {

    static class MyArrayLengthOpenField extends ArrayLengthOpenField {

        @Override
        public int getLength(Object target) {
            return Array.getLength(target);
        }
    }

    static class MyArrayOpenClass extends ArrayOpenClass {

        public MyArrayOpenClass(IOpenClass componentClass) {
            super(componentClass.getSchema(), componentClass, new MyArrayLengthOpenField());
        }

        public IAggregateInfo getAggregateInfo() {
            return aggregateInfo;
        }

        public boolean isAssignableFrom(Class<?> c) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isAssignableFrom(IOpenClass ioc) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean isInstance(Object instance) {
            // TODO Auto-generated method stub
            return false;
        }

        public Object newInstance(IRuntimeEnv env) {
            // TODO Auto-generated method stub
            return null;
        }

        public Iterator<IOpenClass> superClasses() {
            return null;
        }

    }

    static public final DynamicArrayAggregateInfo aggregateInfo = new DynamicArrayAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {
        if (aggregateType instanceof ArrayOpenClass) {
            return ((ArrayOpenClass) aggregateType).getComponentClass();
        }

        return null;
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        IOpenClass componentClass = ((ArrayOpenClass) aggregateType).getComponentClass();

        IOpenField indexField = componentClass.getIndexField();

        if (indexField != null) {
            if (indexField.getType() == indexType) {
                return new ArrayFieldIndex(componentClass, indexField);
            }
        }

        if (indexType != JavaOpenClass.INT) {
            return null;
        }
        return new ArrayIndex(getComponentType(aggregateType));
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dim) {
        if (dim == 0) {
            return componentType;
        }

        IOpenClass[] arrayTypes = ((ADynamicClass) componentType).getArrayTypes();

        synchronized (arrayTypes) {
            if (arrayTypes[dim - 1] != null) {
                return arrayTypes[dim - 1];
            }

            for (int i = 0; i < dim; i++) {
                componentType = new MyArrayOpenClass(componentType);
            }

            return arrayTypes[dim - 1] = componentType;
        }

    }

    public Iterator<Object> getIterator(Object aggregate) {
        // TODO Auto-generated method stub
        return null;
    }

    public boolean isAggregate(IOpenClass type) {
        return type instanceof ArrayOpenClass;
    }

}
