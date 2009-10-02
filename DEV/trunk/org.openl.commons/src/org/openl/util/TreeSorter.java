package org.openl.util;

public abstract class TreeSorter<T> {

    public static <T> void addElement(ITreeElement<T> targetElement, T obj, TreeSorter<T>[] sorters, int level) {
        if (level >= sorters.length) {
            return;
        }

        ITreeElement.Node<T> target = (ITreeElement.Node<T>) targetElement;
        Comparable<?> key = sorters[level].makeKey(obj);
        ITreeElement<T> element = target.getChild(key);
        if (element == null) {
            element = sorters[level].makeElement(obj, 0);
            target.addChild(key, element);
            /*if (level > 0) {
                addSubElements(sorters[level], (ITreeElement.Node<T>) element, obj);
            }*/
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
     * Temp stub method
     **/
    private static void addSubElements(TreeSorter sorter, ITreeElement.Node parent, Object elemmentObject) {
        String eds[] = {"10.10.2007", "11.11.2008", "12.12.2009"};
        for (int i = 0; i < eds.length; i++) {
            ITreeElement ed = sorter.makeFolder(eds[i]);
            parent.addChild(sorter.makeStringKey("ed" + (i + 1)), ed);
            for (int j = 0; j < 3; j++) {
                ITreeElement v = sorter.makeElement(elemmentObject, 0, "" + (j + 1));
                ((ITreeElement.Node) ed).addChild(sorter.makeStringKey("v" + (j + 1)), v);
            }
        }
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
