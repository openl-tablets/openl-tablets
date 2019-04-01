package org.openl.rules.ui.tree;

/**
 * Class that provides several methods for tree building.
 *
 * @param <T> type of node objects
 */
public class TreeBuilder<T> {

    /**
     * Adds new object to target tree node.
     *
     * The algorithm of adding new object to tree is following: the new object is passed to each tree node builder using
     * order in which they are appear in builders array. Tree node builder makes appropriate tree node or nothing if it
     * is not necessary (e.g. builder that makes folder nodes). The new node is added to tree.
     *
     * @param targetNode target node to which will be added new object
     * @param object object to add
     * @param treeNodeBuilders array of tree node builders
     */
    public void addToNode(ITreeNode<T> targetNode, T object, TreeNodeBuilder<T>[] treeNodeBuilders) {
        addToNode(targetNode, object, treeNodeBuilders, 0);
    }

    /**
     * Adds new object to target tree node.
     *
     * The algorithm of adding new object to tree is following: the new object is passed to each tree node builder using
     * order in which they are appear in builders array. Tree node builder makes appropriate tree node or nothing if it
     * is not necessary (e.g. builder that makes folder nodes). The new node is added to tree.
     *
     * @param targetNode target node to which will be added new object
     * @param object object to add
     * @param treeNodeBuilders array of tree node builders
     * @param level index of builder which will be invoked
     */
    public void addToNode(ITreeNode<T> targetNode, T object, TreeNodeBuilder<T>[] treeNodeBuilders, int level) {

        // If level is greater than count of builders finish the adding process
        // (recursion exit condition).
        //
        if (level >= treeNodeBuilders.length) {
            return;
        }

        // Create key for adding object. It used to check that the same node
        // exists.
        //
        Comparable<?> key = treeNodeBuilders[level].makeKey(object);

        ITreeNode element = null;

        // If key is null the rest of building node process should be skipped.
        //
        if (treeNodeBuilders[level].isBuilderApplicableForObject(object) && key != null) {

            // Try to find child node with the same object.
            //
            element = targetNode.getChild(key);

            // If element is null the node with same object is absent.
            //
            if (element == null) {

                // Build new node for the object.
                //
                element = treeNodeBuilders[level].makeNode(object, 0);

                // If element is null then builder has not created the new
                // element
                // and this builder should be skipped.
                // author: Alexey Gamanovich
                //
                if (element != null) {
                    targetNode.addChild(key, element);
                } else {
                    element = targetNode;
                }
            }

            // ///////
            // ???????????????????
            // //////
            else if (treeNodeBuilders[level].isUnique(object)) {

                for (int i = 2; i < 100; ++i) {

                    Comparable<?> key2 = treeNodeBuilders[level].makeKey(object, i);
                    element = targetNode.getChild(key2);

                    if (element == null) {

                        element = treeNodeBuilders[level].makeNode(object, i);

                        // If element is null then sorter has not created the
                        // new
                        // element and this sorter should be skipped.
                        // author: Alexey Gamanovich
                        //
                        if (element != null) {
                            targetNode.addChild(key2, element);
                        } else {
                            element = targetNode;
                        }

                        break;
                    }
                }
            }
        }

        // If node is null skip the current builder: set the targetNode to
        // current element.
        //
        if (element == null) {
            element = targetNode;
        }

        // Invoke the next builder.
        //
        addToNode(element, object, treeNodeBuilders, level + 1);
    }
}
