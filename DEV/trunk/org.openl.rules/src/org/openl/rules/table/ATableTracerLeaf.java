package org.openl.rules.table;

import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.SimpleTracerObject;

public abstract class ATableTracerLeaf extends SimpleTracerObject implements ITableTracerObject {
    
    private Object result;
    
    public ATableTracerLeaf() {
        super();
    }

    public ATableTracerLeaf(Object tracedObject) {
        super(tracedObject);
    }

    public ITableTracerObject[] getTableTracers() {
        ITracerObject[] tracerObjects = getTracerObjects();

        int size = tracerObjects.length;
        ITableTracerObject[] temp = new ITableTracerObject[size];

        System.arraycopy(tracerObjects, 0, temp, 0, size);

        return temp;
    }
    
    public Object getResult() {
        return result;
    }    
    
    public void setResult(Object result) {
        this.result = result;
    }
}
