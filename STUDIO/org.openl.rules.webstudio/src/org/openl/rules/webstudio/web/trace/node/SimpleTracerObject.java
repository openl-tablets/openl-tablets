package org.openl.rules.webstudio.web.trace.node;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleTracerObject implements ITracerObject {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleTracerObject.class);

    private ITracerObject parent;
    private ArrayList<ITracerObject> children;
    private Object result;
    private Throwable error;
    private final String type;

    protected SimpleTracerObject(String type) {
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setParent(ITracerObject parentTraceObject) {
        parent = parentTraceObject;
    }

    @Override
    public ITracerObject getParent() {
        return parent;
    }

    @Override
    public void addChild(ITracerObject child) {
        if (children == null) {
            children = new ArrayList<>();
        }

        children.add(child);
        child.setParent(this);
    }

    @Override
    public Iterable<ITracerObject> getChildren() {
        if (children == null) {
            return Collections.emptyList();
        }
        return children;
    }

    @Override
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
        if (result != null) {
            CachingArgumentsCloner cloner = CachingArgumentsCloner.getInstance();
            Object clonedResult;
            try {
                clonedResult = cloner.deepClone(result);
            } catch (Throwable e) {
                // ignore cloning exception if any, use params itself
                clonedResult = result;
                LOG.debug("Ignored error: ", e);
            }
            this.result = clonedResult;
        } else {
            this.result = null;
        }
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
