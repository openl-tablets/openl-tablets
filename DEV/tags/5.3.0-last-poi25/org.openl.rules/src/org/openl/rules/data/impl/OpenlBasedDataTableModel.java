/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data.impl;

import org.openl.OpenL;
import org.openl.rules.data.IColumnDescriptor;
import org.openl.rules.data.IDataTableModel;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class OpenlBasedDataTableModel implements IDataTableModel {

    String name;
    IOpenClass type;
    OpenL openl;
    IColumnDescriptor[] columnDescriptor;

    public OpenlBasedDataTableModel(String name, IOpenClass type, OpenL openl, IColumnDescriptor[] columnDescriptor) {
        this.name = name;
        this.type = type;
        this.openl = openl;
        this.columnDescriptor = columnDescriptor;
    }

    /**
     *
     */

    public IColumnDescriptor[] getDescriptor() {
        return columnDescriptor;
    }

    /**
     *
     */

    public Class<?> getInstanceClass() {
        return type.getInstanceClass();
    }

    /**
     *
     */

    public String getName() {
        return name;
    }

    /**
     *
     */

    public IOpenClass getType() {
        return type;
    }

    /**
     *
     */

    public Object newInstance() {
        return type.newInstance(openl.getVm().getRuntimeEnv());
    }

}
