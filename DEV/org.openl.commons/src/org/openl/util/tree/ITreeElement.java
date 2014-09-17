package org.openl.util.tree;

public interface ITreeElement<T> {

    Iterable<? extends ITreeElement<T>> getChildren();

    T getObject();

    String getType();

    boolean isLeaf();
}
