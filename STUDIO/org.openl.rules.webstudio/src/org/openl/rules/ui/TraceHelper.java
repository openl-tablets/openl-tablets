/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.openl.rules.table.ITableTracerObject;
import org.openl.util.tree.ITreeElement;
import org.openl.vm.trace.ITracerObject;

/**
 * @author snshor
 */
public class TraceHelper {

    private BidiMap<Integer, ITreeElement<?>> traceTreeCache = new DualHashBidiMap<Integer, ITreeElement<?>>();

    public ITableTracerObject getTableTracer(int elementId) {
        ITreeElement<?> node = traceTreeCache.get(elementId);
        if (node instanceof ITableTracerObject) {
            return (ITableTracerObject) node;
        }
        return null;
    }

    public void cacheTraceTree(ITreeElement<ITracerObject> tree) {
        traceTreeCache.clear();
        cacheTree(tree);
    }

    private void cacheTree(ITreeElement<ITracerObject> treeNode) {
        traceTreeCache.put(traceTreeCache.size(), treeNode);
        Iterable<? extends ITreeElement<ITracerObject>> children = treeNode.getChildren();
        for (ITreeElement<ITracerObject> child : children) {
            cacheTree(child);
        }
    }

    public Integer getNodeKey(ITreeElement<?> node) {
        return traceTreeCache.getKey(node);
    }
}
