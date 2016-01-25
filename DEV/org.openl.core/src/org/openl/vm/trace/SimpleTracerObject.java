package org.openl.vm.trace;

import org.openl.base.INamedThing;

import java.util.ArrayList;
import java.util.Collections;

public abstract class SimpleTracerObject implements ITracerObject {

    private ITracerObject parent;
    private ArrayList<ITracerObject> children;
    private Object result;
    private String type;

    protected SimpleTracerObject(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    public void setParent(ITracerObject parentTraceObject) {
        parent = parentTraceObject;
    }

    public ITracerObject getParent() {
        return parent;
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

    public boolean isLeaf() {
        return children == null;
    }

    public abstract String getUri();

    @Override
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
