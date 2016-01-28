/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.openl.vm.trace.ITracerObject;

/**
 * @author snshor
 */
public class TraceHelper {

    private BidiMap<Integer, ITracerObject> traceTreeCache = new DualHashBidiMap<Integer, ITracerObject>();

    public ITracerObject getTableTracer(int elementId) {
        return traceTreeCache.get(elementId);
    }

    public void cacheTraceTree(ITracerObject tree) {
        traceTreeCache.clear();
        cacheTree(tree);
    }

    private void cacheTree(ITracerObject treeNode) {
        traceTreeCache.put(traceTreeCache.size(), treeNode);
        Iterable<ITracerObject> children = treeNode.getChildren();
        for (ITracerObject child : children) {
            cacheTree(child);
        }
    }

    public Integer getNodeKey(ITracerObject node) {
        return traceTreeCache.getKey(node);
    }
}
