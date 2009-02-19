package org.openl.rules.table;

import org.openl.vm.ITracerObject;

public abstract class ATableTracerLeaf extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    public ITableTracerObject[] getTableTracers() {
        return new ITableTracerObject[]{this};
    }
}
