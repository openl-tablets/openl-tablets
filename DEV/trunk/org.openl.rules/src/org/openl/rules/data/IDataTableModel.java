/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface IDataTableModel {
    
    IColumnDescriptor[] getDescriptor();

    Class<?> getInstanceClass();

    String getName();

    IOpenClass getType();

    Object newInstance();
    
    boolean hasColumnTytleRow();

}
