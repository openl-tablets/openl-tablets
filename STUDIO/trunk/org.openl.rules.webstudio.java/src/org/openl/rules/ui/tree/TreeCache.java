package org.openl.rules.ui.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.util.tree.ITreeElement;

public class TreeCache {

    public TreeCache() {
    }

    private List<ITreeElement<?>> treeElements = new ArrayList<ITreeElement<?>>();

    public ITreeElement<?> get(int index) {
        return (ITreeElement<?>) treeElements.get(index);
    }

    public int getIndex(ITreeElement<?> treeNode) {
        return treeElements.indexOf(treeNode);
    }

    public Collection<?> getAll() {
        return treeElements;
    }

    public boolean put(ITreeElement<?> treeNode) {
        return treeElements.add(treeNode);
    }

    public ITreeElement<?> remove(int index) {
        return treeElements.remove(index);
    }

    public void clear() {
        treeElements.clear();
    }

}
