/*
 * Created on Mar 9, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.types.impl;

import java.util.Iterator;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.IntegerValuesUtils;
import org.openl.util.OpenIterator;

/**
 * @author snshor
 *
 */
public class DynamicArrayAggregateInfo extends AAggregateInfo {
    public static final DynamicArrayAggregateInfo aggregateInfo = new DynamicArrayAggregateInfo();

    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        if (aggregateType instanceof ComponentTypeArrayOpenClass) {
            return aggregateType.getComponentClass();
        }

        return null;
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (indexType == JavaOpenClass.INT || indexType.getInstanceClass() == Integer.class) {
            // if index type is int we return simple java array index.
            return new ArrayIndex(getComponentType(aggregateType));
        } else {
            // we support to work with Datatype arrays like this: people["John"]
            // also different object types may be used as indexes : vehicleSymbols[vehicle]
            IOpenClass componentClass = aggregateType.getComponentClass();
            IOpenField indexField = componentClass.getIndexField();

            if (indexField != null) {
                // If the type of the suggested index is the same as the type of indexed field
                // simply create indexed field
                if (indexField.getType() == indexType) {
                    return new ArrayFieldIndex(componentClass, indexField);
                } else if (IntegerValuesUtils.isIntegerValue(indexField.getType().getInstanceClass()) && String.class
                    .equals(indexType.getInstanceClass())) {
                    // handles the case when index field of Datatype is of type int, and we try to get String index
                    // e.g. person["12"]
                    return new ArrayFieldIndex(componentClass, indexField);
                }
            }
        }
        return null;
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType) {
        return new ComponentTypeArrayOpenClass(componentType);

    }

    @Override
    public Iterator<Object> getIterator(Object aggregate) {
        return OpenIterator.fromArrayObj(aggregate);
    }

    @Override
    public boolean isAggregate(IOpenClass type) {
        return type instanceof ComponentTypeArrayOpenClass;
    }

}
