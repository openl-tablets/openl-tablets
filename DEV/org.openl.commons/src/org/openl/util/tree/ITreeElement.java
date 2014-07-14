package org.openl.util.tree;

import java.util.Iterator;

public interface ITreeElement<T> {

    Iterable<? extends ITreeElement<T>> getChildren();

    T getObject();

    String getType();

    boolean isLeaf();
}
