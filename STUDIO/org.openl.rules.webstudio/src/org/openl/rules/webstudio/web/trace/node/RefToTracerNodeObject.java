package org.openl.rules.webstudio.web.trace.node;

public class RefToTracerNodeObject implements ITracerObject {

    private ITracerObject parent;
    private final ITracerObject originalTracerNode;

    public RefToTracerNodeObject(ITracerObject originalTracerNode) {
        this.originalTracerNode = originalTracerNode;
    }

    @Override
    public ITracerObject getParent() {
        return parent;
    }

    @Override
    public void setParent(ITracerObject parentTraceObject) {
        this.parent = parentTraceObject;
    }

    @Override
    public void addChild(ITracerObject child) {
        //no need to add children to the reference node. It must be competed!
    }

    @Override
    public Iterable<ITracerObject> getChildren() {
        return originalTracerNode.getChildren();
    }

    @Override
    public String getUri() {
        return originalTracerNode.getUri();
    }

    @Override
    public Object[] getParameters() {
        return originalTracerNode.getParameters();
    }

    @Override
    public Object getResult() {
        return originalTracerNode.getResult();
    }

    @Override
    public String getType() {
        return originalTracerNode.getType();
    }

    @Override
    public boolean isLeaf() {
        return originalTracerNode.isLeaf();
    }

    public ITracerObject getOriginalTracerNode() {
        return originalTracerNode;
    }
}
