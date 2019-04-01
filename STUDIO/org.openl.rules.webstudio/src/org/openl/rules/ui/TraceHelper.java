/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;

/**
 * @author snshor
 */
public class TraceHelper {

    private BidiMap<Integer, ITracerObject> traceTreeCache = new DualHashBidiMap<>();

    public ITracerObject getTableTracer(int elementId) {
        return traceTreeCache.get(elementId);
    }

    public void cacheTraceTree(ITracerObject tree) {
        traceTreeCache.clear();
        cacheTree(tree);
    }

    private void cacheTree(ITracerObject treeNode) {
        traceTreeCache.put(traceTreeCache.size(), treeNode);
        if (treeNode instanceof RefToTracerNodeObject) {
            // no need to add children nodes of reference node to the treeCache
            // because they will be added from original node
            return;
        }
        Iterable<ITracerObject> children = treeNode.getChildren();
        for (ITracerObject child : children) {
            cacheTree(child);
        }
    }

    public Integer getNodeKey(ITracerObject node) {
        return traceTreeCache.getKey(node);
    }
}
