package org.openl.util;

public abstract class TreeSorter<T> {

    static public <T> void addElement(ITreeElement<T> targetElement, T obj, TreeSorter<T>[] sorters, int level) {
        if (level >= sorters.length) {
            return;
        }

        ITreeElement.Node<T> target = (ITreeElement.Node<T>) targetElement;
        Comparable<?> key = sorters[level].makeKey(obj);
        ITreeElement<T> element = target.getChild(key);
        if (element == null) {
            element = sorters[level].makeElement(obj, 0);
            target.addChild(key, element);
        } else if (sorters[level].isUnique()) {
            for (int i = 2; i < 100; ++i) {
                Comparable<?> key2 = sorters[level].makeKey(obj, i);
                element = target.getChild(key2);
                if (element == null) {
                    element = sorters[level].makeElement(obj, i);
                    target.addChild(key2, element);
                    break;
                }
            }
        }

        addElement(element, obj, sorters, level + 1);
    }

    /**
     * @return
     */
    protected boolean isUnique() {
        return false;
    }

    public abstract ITreeElement<T> makeElement(Object obj, int i);

    public abstract Comparable<?> makeKey(T obj);

    /**
     * @param obj
     * @param i
     * @return
     */
    protected Comparable<?> makeKey(T obj, int i) {
        throw new UnsupportedOperationException();
    }

}
