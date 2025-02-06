/**
 * OpenL Tablets,  2006
 * https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.rules.ui;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;

import org.openl.rules.webstudio.web.trace.TreeBuildTracer;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.trace.node.LazyTracerNodeObject;
import org.openl.rules.webstudio.web.trace.node.RefToTracerNodeObject;

/**
 * @author snshor
 */
public class TraceHelper {

    private final BidiMap<Integer, ITracerObject> traceTreeCache = new DualHashBidiMap<>();

    public ITracerObject getTableTracer(int elementId) {
        var node = traceTreeCache.get(elementId);
        if (node != null) {
            initializeLazyChildren(node);
        }
        return node;
    }

    private void initializeLazyChildren(ITracerObject node) {
        List<LazyTracerNodeObject> lazyNodes = new ArrayList<>();
        for (var childNode : node.getChildren()) {
            if (childNode instanceof LazyTracerNodeObject) {
                lazyNodes.add((LazyTracerNodeObject) childNode);
            }
        }
        for (var lazyNode : lazyNodes) {
            var realNode = TreeBuildTracer.performLazyTracerNode(lazyNode);
            replaceLazyNodeAndCacheChild(lazyNode, realNode);
        }
    }

    public void cacheTraceTree(ITracerObject tree) {
        traceTreeCache.clear();
        cacheTree(tree);
    }

    private void replaceLazyNodeAndCacheChild(LazyTracerNodeObject lazyNode, ITracerObject realNode) {
        Integer key = traceTreeCache.getKey(lazyNode);
        traceTreeCache.remove(key);
        traceTreeCache.put(key, realNode);
        for (ITracerObject child : realNode.getChildren()) {
            cacheTree(child);
        }
    }

    private void cacheTree(ITracerObject treeNode) {
        traceTreeCache.put(traceTreeCache.size(), treeNode);
        if (treeNode instanceof RefToTracerNodeObject || treeNode instanceof LazyTracerNodeObject) {
            // 1. no need to add children nodes of reference node to the treeCache
            //    because they will be added from original node
            // 2. no need to add children nodes of lazy node to the treeCache they will be initialized later
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
