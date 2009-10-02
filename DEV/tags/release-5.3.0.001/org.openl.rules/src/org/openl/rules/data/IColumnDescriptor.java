/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.Map;

import org.openl.OpenlToolAdaptor;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.BoundError;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface IColumnDescriptor {

    Object getColumnValue(Object target);

    IString2DataConvertor getConvertor();

    /**
     * @return
     */
    String getDisplayName();

    // IOpenClass getType();

    Object getLink(IOpenClass fieldType, ILogicalTable values, IDataBase db, IBindingContext cxt) throws Exception;

    Object getLiteral(IOpenClass paramType, ILogicalTable values, OpenlToolAdaptor ota) throws Exception;

    String getName();

    /**
     * @return
     */
    IOpenClass getType();

    Map<String, Integer> getUniqueIndex(ITable table, int idx) throws BoundError;

    boolean isConstructor();

    boolean isReference();

    void populateLink(Object target, ILogicalTable values, IDataBase db, IBindingContext cxt) throws Exception;

    void populateLiteral(Object target, ILogicalTable values, OpenlToolAdaptor ota) throws Exception;

}
