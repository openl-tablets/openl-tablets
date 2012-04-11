/*
 * Created on Jun 18, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types;

/**
 * @author snshor
 *
 * This class supports index operator (x[i]) by providing element access
 */
public interface IOpenIndex {
    IOpenClass getElementType();

    IOpenClass getIndexType();

    Object getValue(Object container, Object index);

    boolean isWritable();

    void setValue(Object container, Object index, Object value);

}
