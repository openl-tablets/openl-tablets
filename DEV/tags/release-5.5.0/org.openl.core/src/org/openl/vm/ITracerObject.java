/**
 * Created Dec 3, 2006
 */
package org.openl.vm;

import java.util.ArrayList;
import java.util.Iterator;

import org.openl.base.INamedThing;
import org.openl.util.AOpenIterator;
import org.openl.util.tree.ITreeElement;

/**
 * @author snshor
 *
 */
public interface ITracerObject extends ITreeElement<ITracerObject>, INamedThing {

    public static abstract class SimpleTracerObject implements ITracerObject {
        Object traceObject;

        ArrayList<ITracerObject> children;

        private ITracerObject parent;

        public SimpleTracerObject() {
        }

        public SimpleTracerObject(Object traceObject) {
            this.traceObject = traceObject;
        }

        public void addChild(ITracerObject child) {
            if (children == null) {
                children = new ArrayList<ITracerObject>();
            }
            children.add(child);
        }

        public Iterator<ITracerObject> getChildren() {
            if (children == null) {
                return AOpenIterator.empty();
            }

            return children.iterator();
        }

        public String getName() {
            return getDisplayName(INamedThing.SHORT);
        }

        public ITracerObject getObject() {
            return this;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.vm.ITracerObject#getParent()
         */
        public ITracerObject getParent() {
            return parent;
        }

        public Object getTraceObject() {
            return traceObject;
        }

        public ITracerObject[] getTracerObjects() {
            return children == null ? EMPTY : (ITracerObject[]) children.toArray(EMPTY);
        }

        public abstract String getUri();

        public boolean isLeaf() {
            return children == null;
        }

        /*
         * (non-Javadoc)
         *
         * @see org.openl.vm.ITracerObject#setParent(org.openl.vm.ITracerObject)
         */
        public void setParent(ITracerObject parentTraceObject) {
            parent = parentTraceObject;
        }

    }

    static final public ITracerObject[] EMPTY = {};

    public void addChild(ITracerObject child);

    /**
     * Get parent trace object.
     *
     * @return Parent <code>ITracerObject</code>.
     */
    ITracerObject getParent();

    public Object getTraceObject();

    public ITracerObject[] getTracerObjects();

    public String getUri();

    /**
     * Set parent trace object.
     *
     * @param Parent <code>ITracerObject</code>.
     */
    void setParent(ITracerObject parentTraceObject);

}
