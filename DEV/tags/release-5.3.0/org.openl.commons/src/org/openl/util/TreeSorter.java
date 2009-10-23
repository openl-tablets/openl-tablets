package org.openl.util;

public abstract class TreeSorter<T> {

    public static <T> void addElement(ITreeElement<T> targetElement, T obj, TreeSorter<T>[] sorters, int level) {
        if (level >= sorters.length) {
            return;
        }

        ITreeElement.Node<T> target = (ITreeElement.Node<T>) targetElement;
        Comparable<?> key = sorters[level].makeKey(obj);
        ITreeElement<T> element = null;
        
        if (key != null) {

            element = target.getChild(key);

            if (element == null) {

                element = sorters[level].makeElement(obj, 0);

                // If element is null then sorter has not created the new
                // element
                // and this sorter should be skipped.
                // author: Alexey Gamanovich
                //
                if (element != null) {
                    target.addChild(key, element);
                } else {
                    element = targetElement;
                }

            } else if (sorters[level].isUnique()) {

                for (int i = 2; i < 100; ++i) {

                    Comparable<?> key2 = sorters[level].makeKey(obj, i);
                    element = target.getChild(key2);

                    if (element == null) {

                        element = sorters[level].makeElement(obj, i);

                        // If element is null then sorter has not created the
                        // new
                        // element and this sorter should be skipped.
                        // author: Alexey Gamanovich
                        //
                        if (element != null) {
                            target.addChild(key2, element);
                        } else {
                            element = targetElement;
                        }

                        break;
                    }
                }
            }
        }

        if (element == null) {
            element = targetElement;
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

    public abstract ITreeElement<T> makeElement(Object obj, int i, String displayName);

    public abstract ITreeElement<T> makeFolder(String name);

    public abstract Comparable<?> makeKey(T obj);

    public abstract Comparable<?> makeStringKey(String key);

    /**
     * @param obj
     * @param i
     * @return
     */
    protected Comparable<?> makeKey(T obj, int i) {
        throw new UnsupportedOperationException();
    }

}
