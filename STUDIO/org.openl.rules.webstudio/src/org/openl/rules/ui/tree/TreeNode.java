package org.openl.rules.ui.tree;

import java.util.Map;
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
    private Map<Object, ITreeNode<T>> elements = new TreeMap<>();

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
    @Override
    public void addChild(Object key, ITreeNode<T> child) {
        elements.put(key, child);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITreeNode<T> getChild(Object key) {
        return elements.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterable<? extends org.openl.util.tree.ITreeElement<T>> getChildren() {
        return elements.values();
    }

    /**
     * Gets the map of elements.
     * 
     * @return map of elements
     */
    public Map<Object, ITreeNode<T>> getElements() {
        return elements;
    }

    public void setElements(Map<Object, ITreeNode<T>> elements) {
        this.elements = elements;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public T getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Checks that node is leaf.
     * 
     * @return <code>true</code> if node is leaf; <code>false</code> - otherwise
     */
    @Override
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
