package org.openl.rules.ui.tree;

/**
 * Provides methods for tree node building. 
 *
 * @param <T> type of node object
 */
public interface TreeNodeBuilder<T extends Object> {
    
    Comparable<?> makeKey(T object);
    ITreeNode<Object> makeNode(T object, int i);

    Comparable<?> makeKey(T object, int i);
    boolean isUnique();
}
