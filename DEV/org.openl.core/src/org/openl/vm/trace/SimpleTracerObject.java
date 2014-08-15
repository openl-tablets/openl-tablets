package org.openl.vm.trace;

import java.util.ArrayList;
import java.util.Collections;

import org.openl.base.INamedThing;

public abstract class SimpleTracerObject implements ITracerObject {
    
    private Object traceObject;
    
    private ITracerObject parent;
    private ArrayList<ITracerObject> children;

    public SimpleTracerObject() {
    }

    public SimpleTracerObject(Object traceObject) {
        this.traceObject = traceObject;
    }

    public void setParent(ITracerObject parentTraceObject) {
        parent = parentTraceObject;
    }

    public ITracerObject getParent() {
        return parent;
    }

    public Object getTraceObject() {
        return traceObject;
    }

    public void addChild(ITracerObject child) {
        if (children == null) {
            children = new ArrayList<ITracerObject>();
        }

        children.add(child);
    }

    public Iterable<? extends org.openl.util.tree.ITreeElement<ITracerObject>> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }

    public String getName() {
        return getDisplayName(INamedThing.SHORT);
    }

    public ITracerObject getObject() {
        return this;
    }

    public ITracerObject[] getTracerObjects() {
     
        if (children == null) {
            return new ITracerObject[0];
        }
        
        return (ITracerObject[]) children.toArray(new ITracerObject[children.size()]);
    }

    public boolean isLeaf() {
        return children == null;
    }

    public abstract String getUri();
}