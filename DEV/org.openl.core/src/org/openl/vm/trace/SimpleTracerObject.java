package org.openl.vm.trace;

import org.openl.base.INamedThing;

import java.util.ArrayList;
import java.util.Collections;

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
        child.setParent(this);
    }

    public Iterable<ITracerObject> getChildren() {
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

    public boolean isLeaf() {
        return children == null;
    }

    public abstract String getUri();
}