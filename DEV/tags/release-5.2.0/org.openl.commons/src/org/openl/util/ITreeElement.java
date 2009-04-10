package org.openl.util;

import java.util.Iterator;

public interface ITreeElement<T> {
    static public interface Node<T> extends ITreeElement<T> {
        boolean addChild(Object key, ITreeElement<T> child);

        ITreeElement<T> getChild(Object key);
    }

    // TreeMap getElementsx();
    Iterator<T> getChildren();

    // String getDisplayValue();
    T getObject();

    String getType();

    boolean isLeaf();
}
