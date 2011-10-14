package org.openl.util.tree;

import java.util.Iterator;

public interface ITreeElement<T> {
  
    Iterator<? extends ITreeElement<T>> getChildren();
    T getObject();
    String getType();
    boolean isLeaf();
}
