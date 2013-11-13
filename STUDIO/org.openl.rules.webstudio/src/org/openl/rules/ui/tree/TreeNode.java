package org.openl.rules.ui.tree;

import java.util.Iterator;
import java.util.TreeMap;

/**
 * Class that represents tree node. Used as container for specified object.
 * 
 * @param <T> type of object
 */
public class TreeNode<T> implements ITreeNode<T> {

    /**
     * Children of current node.
     */
    private TreeMap<Object, ITreeNode<T>> elements = new TreeMap<Object, ITreeNode<T>>();

    /**
     * String that represent the node type.
     */
    private String type;

    /**
     * Contained object.
     */
    private T object;

    /**
     *{@inheritDoc}
     */
    public void addChild(Object key, ITreeNode<T> child) {
        elements.put(key, child);
    }

    /**
     * {@inheritDoc}
     */
    public ITreeNode<T> getChild(Object key) {
        return elements.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<ITreeNode<T>> getChildren() {
        return elements.values().iterator();
    }

    /**
     * Gets the map of elements.
     * 
     * @return map of elements
     */
    public TreeMap<Object, ITreeNode<T>> getElements() {
        return elements;
    }

    /**
     * {@inheritDoc}
     */
    public T getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public String getType() {
        return type;
    }

    /**
     * Checks that node is leaf.
     * 
     * @return <code>true</code> if node is leaf; <code>false</code> - otherwise
     */
    public boolean isLeaf() {

        return elements == null || elements.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public void setObject(T object) {
        this.object = object;
    }

    /**
     * Sets type of node.
     * 
     * @param type string that indicates type of node
     */
    public void setType(String type) {
        this.type = type;
    }
}
