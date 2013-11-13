/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import org.openl.OpenL;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class OpenlBasedDataTableModel implements ITableModel {

    private String name;
    private IOpenClass type;
    private OpenL openl;
    private ColumnDescriptor[] columnDescriptor;
    private boolean hasColumnTitleRow; 

    public OpenlBasedDataTableModel(String name, IOpenClass type, OpenL openl, ColumnDescriptor[] columnDescriptor, boolean hasColumnTitleRow) {
        this.name = name;
        this.type = type;
        this.openl = openl;
        this.columnDescriptor = columnDescriptor;
        this.hasColumnTitleRow = hasColumnTitleRow;
    }

    public boolean hasColumnTitleRow() {
        return hasColumnTitleRow;
    }

    public ColumnDescriptor[] getDescriptor() {
        return columnDescriptor;
    }

    public Class<?> getInstanceClass() {
        return type.getInstanceClass();
    }

    public String getName() {
        return name;
    }

    public IOpenClass getType() {
        return type;
    }

    public Object newInstance() {
        return type.newInstance(openl.getVm().getRuntimeEnv());
    }

}
