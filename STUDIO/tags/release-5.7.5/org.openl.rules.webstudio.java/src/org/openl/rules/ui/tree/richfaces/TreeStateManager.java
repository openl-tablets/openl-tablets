package org.openl.rules.ui.tree.richfaces;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.richfaces.component.UITree;
import org.richfaces.component.state.TreeStateAdvisor;
import org.richfaces.model.TreeRowKey;

/**
 * @author Andrei Astrouski
 */
public class TreeStateManager implements TreeStateAdvisor {

    public static final String TREE_NODE_KEY_SEPARATOR = ":";

    private String nodeToOpen;
    private String[] nodePathToOpen;
    private int openedNodesNumber = 0;

    public TreeStateManager() {
    }

    public TreeStateManager(String nodeToOpen) {
        setNodeToOpen(nodeToOpen);
    }

    public void setNodeToOpen(String nodeToOpen) {
        this.nodeToOpen = nodeToOpen;
        if (StringUtils.isNotBlank(nodeToOpen)) {
            nodePathToOpen = nodeToOpen.split(TREE_NODE_KEY_SEPARATOR);
        }
    }

    public Boolean adviseNodeOpened(UITree tree) {
        if (!ArrayUtils.isEmpty(nodePathToOpen)) {
            String currentNode = ((TreeRowKey<?>) tree.getRowKey()).getPath();
            // we must open all parent nodes
            String nodeToOpen = "";
            int nodesToOpenNumber = nodePathToOpen.length;
            if (nodesToOpenNumber > openedNodesNumber) {
                for (int i = 0; i < openedNodesNumber + 1; i++) {
                    nodeToOpen += nodePathToOpen[i];
                    if (i < openedNodesNumber) {
                        nodeToOpen += TREE_NODE_KEY_SEPARATOR;
                    }
                }
            }
            if (nodeToOpen.equals(currentNode)) {
                openedNodesNumber++;
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    public Boolean adviseNodeSelected(UITree tree) {
        if (StringUtils.isNotBlank(nodeToOpen)) {
            String currentNode = ((TreeRowKey<?>) tree.getRowKey()).getPath();
            if (nodeToOpen.equals(currentNode)) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}
