/**
 * Created Dec 3, 2006
 */
package org.openl.vm.trace;

import org.openl.base.INamedThing;
import org.openl.util.tree.ITreeElement;

/**
 * @author snshor
 */
public interface ITracerObject extends ITreeElement<ITracerObject>, INamedThing {

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

    @Override
    Iterable<ITracerObject> getChildren();

    Object getTraceObject();

    String getUri();

    Object getResult();
}
