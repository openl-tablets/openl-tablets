/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

/**
 * @author snshor
 */
public interface ITracerObject {

    /**
     * Get parent trace object.
     *
     * @return Parent <code>ITracerObject</code>.
     */
    ITracerObject getParent();

    /**
     * Set parent trace object.
     *
     * @param parentTraceObject <code>ITracerObject</code>.
     */
    void setParent(ITracerObject parentTraceObject);

    void addChild(ITracerObject child);

    Iterable<ITracerObject> getChildren();

    String getUri();

    Object getResult();

    ITracerObject getObject();

    String getType();

    boolean isLeaf();
}
