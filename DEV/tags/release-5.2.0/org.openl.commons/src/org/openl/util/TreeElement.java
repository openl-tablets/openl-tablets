package org.openl.util;

import java.util.Iterator;
import java.util.TreeMap;

public class TreeElement implements ITreeElement.Node {
    TreeMap elements = new TreeMap();

    String type;

    Object object;

    public TreeElement(String type) {
        this.type = type;
    }

    public boolean addChild(Object key, ITreeElement child) {
        return elements.put(key, child) != null;
    }

    public ITreeElement getChild(Object key) {
        return (ITreeElement) elements.get(key);
    }

    public Iterator getChildren() {
        return elements.values().iterator();
    }

    public TreeMap getElements() {
        return elements;
    }

    public Object getObject() {
        return object;
    }

    public String getType() {
        return type;
    }

    public boolean isLeaf() {
        return elements == null || elements.size() == 0;
    }

    public void setElements(TreeMap children) {
        elements = children;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public void setType(String type) {
        this.type = type;
    }
}
