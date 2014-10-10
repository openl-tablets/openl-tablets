package org.openl.binding.impl;

import java.util.Comparator;

public class NodeUsageComparator implements Comparator<NodeUsage> {
    @Override
    public int compare(NodeUsage o1, NodeUsage o2) {
        return o1.getStart() - o2.getStart();
    }
}
