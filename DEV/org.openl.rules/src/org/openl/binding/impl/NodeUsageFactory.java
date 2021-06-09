package org.openl.binding.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openl.binding.IBoundNode;
import org.openl.util.CollectionUtils;

/**
 * Creates {@link NodeUsage} list for given bound node and source code string
 *
 * @author Vladyslav Pikus
 */
public final class NodeUsageFactory {

    private static final List<NodeUsageCreator> CREATORS = Arrays.asList(MethodBoundNodeUsageCreator.getInstance(),
        FieldBoundNodeUsageCreator.getInstance(),
        TypeNodeUsageCreator.getInstance());

    private NodeUsageFactory() {
    }

    /**
     *  Create {@link NodeUsage} list for given bound node and source code string
     *
     * @param boundNode bound node to convert
     * @param sourceString source code
     * @param startIndex start index
     * @return node usage list
     */
    public static List<NodeUsage> createNodeUsageList(IBoundNode boundNode, String sourceString, int startIndex) {
        List<NodeUsage> nodeUsages = new ArrayList<>();
        findNodeUsages(nodeUsages, boundNode, sourceString, startIndex);
        nodeUsages.sort(Comparator.comparingInt(NodeUsage::getStart));
        return Collections.unmodifiableList(nodeUsages);
    }

    private static void findNodeUsages(List<NodeUsage> nodeUsages,
            IBoundNode boundNode,
            String sourceString,
            int startIndex) {
        if (boundNode == null) {
            return;
        }

        for (NodeUsageCreator creator : CREATORS) {
            if (creator.accept(boundNode)) {
                creator.create(boundNode, sourceString, startIndex).ifPresent(nodeUsages::add);
            }
        }

        IBoundNode[] children = boundNode.getChildren();
        if (CollectionUtils.isNotEmpty(children)) {
            for (IBoundNode child : children) {
                findNodeUsages(nodeUsages, child, sourceString, startIndex);
            }
        }
        if (boundNode instanceof ATargetBoundNode) {
            IBoundNode targetNode = boundNode.getTargetNode();
            if (targetNode != null) {
                findNodeUsages(nodeUsages, targetNode, sourceString, startIndex);
            }
        }
    }
}
