package org.openl.rules.webstudio.web.trace.node;

import java.util.ArrayList;
import java.util.Collections;

public abstract class SimpleTracerObject implements ITracerObject {

    private ITracerObject parent;
    private ArrayList<ITracerObject> children;
    private Object result;
    private Throwable error;
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
            children = new ArrayList<>();
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

    @Override
    public Object[] getParameters() {
        return null;
    }

    @Override
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public boolean hasError() {
        return error != null;
    }

}
