/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.Iterator;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;
import org.openl.types.impl.AAggregateInfo;

/**
 * @author snshor
 *
 */
public class JavaNoAggregateInfo extends AAggregateInfo {

    static final public JavaNoAggregateInfo NO_AGGREGATE = new JavaNoAggregateInfo();

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IAggregateInfo#getComponentType(org.openl.types.IOpenClass)
     */
    @Override
    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IAggregateInfo#getIndex(org.openl.types.IOpenClass)
     */
    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IAggregateInfo#getIterator(java.lang.Object)
     */
    @Override
    public Iterator<Object> getIterator(Object aggregate) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IAggregateInfo#isAggregate()
     */
    @Override
    public boolean isAggregate(IOpenClass type) {
        return false;
    }

}
