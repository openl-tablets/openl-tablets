/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.rules.data.impl.ColumnDescriptor;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface IDataTableModel {
    
    ColumnDescriptor[] getDescriptor();

    Class<?> getInstanceClass();

    String getName();

    IOpenClass getType();

    Object newInstance();
    
    boolean hasColumnTytleRow();

}
