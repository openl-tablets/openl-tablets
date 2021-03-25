package org.openl.binding.impl;

import java.util.Arrays;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.util.CollectionUtils;

/**
 * @author Vladyslav Pikus
 */
public final class NodeUsageSearcher {

    private static final List<NodeUsageCreator> FACTORIES = Arrays.asList(ArrayBoundNodeUsageCreator.getInstance());

    private NodeUsageSearcher() {

    }

    public static void findTypes(List<NodeUsage> nodeUsages,
            IBoundNode boundNode,
            String sourceString,
            int startIndex) {
        if (boundNode == null) {
            return;
        }

        for (NodeUsageCreator factory : FACTORIES) {
            if (factory.accept(boundNode)) {
                factory.create(boundNode, sourceString, startIndex).ifPresent(nodeUsages::add);
            }
        }

        IBoundNode[] children = boundNode.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            for (IBoundNode child : children) {
                findTypes(nodeUsages, child, sourceString, startIndex);
            }
        }
        if (boundNode instanceof ATargetBoundNode) {
            IBoundNode targetNode = boundNode.getTargetNode();
            if (targetNode != null) {
                findTypes(nodeUsages, targetNode, sourceString, startIndex);
            }
        }
    }
}
