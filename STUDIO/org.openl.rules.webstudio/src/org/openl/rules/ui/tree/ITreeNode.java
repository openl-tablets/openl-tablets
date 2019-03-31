package org.openl.rules.ui.tree;

import org.openl.util.tree.ITreeElement;

/**
 * Class that represents the tree node abstraction.
 * 
 * @param <T> type of node object
 */
public interface ITreeNode<T> extends ITreeElement<T> {

    /**
     * Adds child to node using its key.
     * 
     * @param key node key
     * @param child tree node to add
     * @return
     */
    void addChild(Object key, ITreeNode<T> child);

    /**
     * Gets child of node using its key.
     * 
     * @param key child key
     * @return tree node if exists; <code>null</code> - otherwise;
     */
    ITreeNode<T> getChild(Object key);
}
