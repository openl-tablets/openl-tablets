package org.openl.rules.table;

import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerLeaf extends SimpleTracerObject implements ITableTracerObject {

    private Object result;

    public ATableTracerLeaf() {
        super();
    }

    public ATableTracerLeaf(Object tracedObject) {
        super(tracedObject);
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
