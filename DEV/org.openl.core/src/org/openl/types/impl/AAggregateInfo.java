/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import java.lang.reflect.Array;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 * This class provides standard Java metaphor of array implementation
 */
public abstract class AAggregateInfo implements IAggregateInfo {

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return getIndex(aggregateType, JavaOpenClass.INT);
    }

    public IOpenClass getIndexedAggregateType(IOpenClass componentType) {
        return getArrayType(componentType);
    }

    public static IOpenClass getArrayType(IOpenClass componentType) {
        Object ary = Array.newInstance(componentType.getInstanceClass(), 0);
        return JavaOpenClass.getOpenClass(ary.getClass());
    }

    public Object makeIndexedAggregate(IOpenClass componentClass, int size) {
        return Array.newInstance(componentClass.getInstanceClass(), size);
    }

}
