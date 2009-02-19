package org.openl.rules.table;

import org.openl.vm.ITracerObject;

public abstract class ATableTracerLeaf extends ITracerObject.SimpleTracerObject implements ITableTracerObject {
    public ATableTracerLeaf(){
        super();
    }
    
    public ATableTracerLeaf(Object tracedObject){
        super(tracedObject);
    }
    
    public ITableTracerObject[] getTableTracers() {
        return new ITableTracerObject[]{this};
    }
}
