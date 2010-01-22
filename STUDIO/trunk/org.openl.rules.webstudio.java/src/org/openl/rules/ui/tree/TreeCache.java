package org.openl.rules.ui.tree;

import java.util.Collection;

import org.openl.rules.ui.ObjectMap;
import org.openl.util.tree.ITreeElement;

public class TreeCache {

    public TreeCache() {
    }

    private ObjectMap indexTreeMap = new ObjectMap();

    public ITreeElement<?> get(int index) {
        return (ITreeElement<?>) indexTreeMap.getObject(index);
    }

    @SuppressWarnings("unchecked")
    public int getIndex(ITreeElement<?> treeNode) {
        return indexTreeMap.getID(treeNode);
    }

    public Collection<?> getAll() {
        return indexTreeMap.getValues();
    }

    @SuppressWarnings("unchecked")
    public int put(ITreeElement<?> treeNode) {
        return indexTreeMap.getNewID(treeNode);
    }

    public int remove(int index) {
        return indexTreeMap.remove(index);
    }

    public void clear() {
        indexTreeMap.reset();
    }
}
