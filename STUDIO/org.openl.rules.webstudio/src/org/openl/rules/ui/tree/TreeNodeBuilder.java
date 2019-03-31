package org.openl.rules.ui.tree;

/**
 * Provides methods for tree node building.
 *
 * @param <T> type of node object
 */
public interface TreeNodeBuilder<T extends Object> {

    /**
     * Generates comparable key that will be used for detecting similar nodes.
     * 
     * @param object The object for key generating.
     * @return Key for object.
     */
    Comparable<?> makeKey(T object);

    /**
     * @param object The object that will be displayed in the tree
     * @param i Order number of object.
     * @return TreeNode generated for the object.
     */
    ITreeNode<Object> makeNode(T object, int i);

    /**
     * Generates comparable key for similar keys. It only used if <code>isUnique==true</code>. Then we have to create
     * nodes for each similar object. The key will be generated with using of order number of key. (The first node have
     * number <code>0</code>, the second - <code>2</code>, the third <code>3</code> etc.)
     * 
     * @param object The object for displaying in the tree.
     * @param i Order number of object.
     * @return Key for object.
     */
    Comparable<?> makeKey(T object, int i);

    /**
     * @param object The object that can be processed by tree builder.
     * @return <code>true</code> if specified object must be processed by tree builder <code>false</code> if current
     *         tree builder must skip object.
     */
    boolean isBuilderApplicableForObject(T object);

    /**
     * @param object The object to check.
     * @return <code>true</code> if node is obligatory to display even if the tree already has element with similar key
     */
    boolean isUnique(T object);
}
