package org.openl.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/*
 * Created on May 12, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */

/**
 * @author snshor
 */
public class SkipList<K,V> implements Map<K,V> {
    
    static public interface ISkipListNode<K,V> extends Map.Entry<K,V> {
        // SkipListNode previous(int level);
        int getLevel();

        ISkipListNode<K,V> next(int level);

        void setNext(ISkipListNode<K,V> node, int level);

    }

    static class SkipListNode<K,V> implements ISkipListNode<K,V> {

        ISkipListNode<K,V>[] nodes;
        K key;
        V value;

        /**
         *
         */

        @SuppressWarnings("unchecked")
        SkipListNode(K key, V value, int level) {
            this.key = key;
            this.value = value;
            nodes = new ISkipListNode[level];
        }

        /**
         *
         */

        public K getKey() {
            return key;
        }

        public int getLevel() {
            return nodes.length;
        }

        /**
         *
         */

        public V getValue() {
            return value;
        }

        /**
         *
         */

        public ISkipListNode<K,V> next(int level) {
            return nodes[level];
        }

        /**
         *
         */

        public void setNext(ISkipListNode<K,V> node, int level) {
            nodes[level] = node;
        }

        /**
         *
         */

        public V setValue(V value) {
            // TODO Auto-generated method stub
            return null;
        }

    }
    Comparator<K> keyComparator;
    double nodeRatio;

    int maxIndexLevel;

    int indexLevel = 0;

    ISkipListNode<K,V> header;

    int size = 0;

    /**
     *
     */

    Random random = new Random(0);

    /**
     *
     */
    public SkipList() {
        this(null, 0.3, 10);

    }

    public SkipList(Comparator<K> keyComparator, double nodeRatio, int maxLevel) {
        this.keyComparator = keyComparator;
        this.nodeRatio = nodeRatio;
        maxIndexLevel = maxLevel;
        clear();
    }

    /**
     *
     */

    public void clear() {
        header = makeSkipListNode(null, null, maxIndexLevel + 1);
        size = 0;
        indexLevel = 0;
    }

    final int compare(K myKey, K key) {
        
        if (keyComparator == null) {
            return 0;
        }
        
        return myKey == null ? -1 : keyComparator.compare(myKey, key);
    }

    /**
     *
     */

    @SuppressWarnings("unchecked")
    public boolean containsKey(Object key) {
        K k = (K)key;
        ISkipListNode<K,V> inode = findNodeGE(k);
        return hasKey(inode, k);
    }

    /**
     *
     */

    public boolean containsValue(Object value) {

        for (ISkipListNode<K,V> node = header.next(0); node != null; node = node.next(0)) {
            if (value.equals(node.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     */

    public Set<Map.Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    ISkipListNode<K,V> findNodeGE(K searchKey) {
        ISkipListNode<K,V> x = header;
        // loop invariant: x.key < searchKey
        for (int i = indexLevel; i >= 0; --i) {
            while (x.next(i) != null && compare(x.next(i).getKey(), searchKey) < 0) {
                x = x.next(i);
            }
        }

        x = x.next(0);

        return x;
    }

    /**
     *
     */

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        
        K k = (K)key;
        ISkipListNode<K,V> inode = findNodeGE(k);

        if (hasKey(inode, k)) {
            return inode.getValue();
        }
        return null;
    }

    final boolean hasKey(ISkipListNode<K,V> inode, K key) {
        return inode == null || inode == header ? false : inode.getKey().equals(key);
    }

    /**
     *
     */

    public boolean isEmpty() {
        return size == 0;
    }

    /**
     *
     */

    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    protected ISkipListNode<K,V> makeSkipListNode(K key, V value, int level) {
        return new SkipListNode<K,V>(key, value, level + 1);
    }

    /**
     *
     */

    @SuppressWarnings("unchecked")
    public V put(K searchKey, V newValue) {
        ISkipListNode<K,V>[] path = new ISkipListNode[maxIndexLevel + 1];
        ISkipListNode<K,V> x = header;
        for (int i = indexLevel; i >= 0; --i) {
            while (x.next(i) != null && compare(x.next(i).getKey(), searchKey) < 0) {
                x = x.next(i);
            }
            path[i] = x;
        }
        // -- x®key < searchKey £x®forward[i]®key
        x = x.next(0);
        if (hasKey(x, searchKey)) {
            V oldValue = x.getValue();
            x.setValue(newValue); // x®key = searchKey then x®value :=
                                    // newValue
            return oldValue;
        } else {
            ++size;
            int lvl = randomLevel();
            if (lvl > indexLevel) {
                for (int i = indexLevel + 1; i <= lvl; ++i) {
                    path[i] = header;
                }
                indexLevel = lvl;
            }
            x = makeSkipListNode(searchKey, newValue, lvl);
            for (int i = 0; i <= lvl; ++i) {
                x.setNext(path[i].next(i), i);
                path[i].setNext(x, i);
            }
        }
        return null;
    }

    public void putAll(Map<? extends K,? extends V> t) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     * @return
     */
    private int randomLevel() {

        int max = Math.min(maxIndexLevel, indexLevel + 1);
        int i = 0;
        for (; i < max; ++i) {
            double d = random.nextDouble();
            if (d > nodeRatio) {
                return i;
            }
        }

        return i;
    }

    /**
     *
     */

    public V remove(Object key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

    /**
     *
     */

    public int size() {
        return size;
    }

    /**
     *
     */

    public Collection<V> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException();
    }

}
