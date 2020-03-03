package org.openl.util.tree;

import java.util.Collection;

public interface ITreeElement<T> {

    Collection<? extends ITreeElement<T>> getChildren();

    T getObject();

    String getType();

    boolean isLeaf();
}
