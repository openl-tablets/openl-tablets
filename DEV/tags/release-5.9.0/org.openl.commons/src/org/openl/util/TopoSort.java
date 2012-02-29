/*
 * Created on Nov 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * @author snshor
 *
 */
public final class TopoSort<T> {

    static class Counter {
        int cnt = 0;
    }

    public interface IPair<T> {
        T getLeaf();

        T getRoot();
    }

    ArrayList<T> roots = new ArrayList<T>();

    HashMap<T, Counter> leaves = new HashMap<T, Counter>();

    HashMap<T, List<T>> dependents = new HashMap<T, List<T>>();
    /**
     * This method takes Nx2-dimensional matrix symbolizing dependency graph,
     * and topologically sorts it
     *
     * @param roots list of
     * @param leaves
     * @return
     */
    static public <T> List<T> sort(IPair<T>[] pairs) throws TopoSortCycleException {
        int len = pairs.length;
        TopoSort<T> ts = new TopoSort<T>();
        for (int i = 0; i < len; i++) {
            ts.addOrderedPair(pairs[i].getRoot(), pairs[i].getLeaf());
        }

        return ts.sort();
    }
    /**
     * This method takes 2 equal size lists symbolizing dependency graph, and
     * topologically sorts it
     *
     * @param roots list of
     * @param leaves
     * @return
     */
    static public <T> List<T> sort(List<T> roots, List<T> leaves) throws TopoSortCycleException {
        int len = roots.size();
        TopoSort<T> ts = new TopoSort<T>();
        for (int i = 0; i < len; i++) {
            ts.addOrderedPair(roots.get(i), leaves.get(i));
        }

        return ts.sort();
    }

    /**
     * This method takes 2 equal size arrays symbolizing dependency graph, and
     * topologically sorts it
     *
     * @param roots list of
     * @param leaves
     * @return
     */
    static public <T> List<T> sort(T[] aryRoots, T[] aryLeaves) throws TopoSortCycleException {
        int len = aryRoots.length;
        TopoSort<T> ts = new TopoSort<T>();
        for (int i = 0; i < len; i++) {
            ts.addOrderedPair(aryRoots[i], aryLeaves[i]);
        }

        return ts.sort();
    }

    /**
     * This method takes Nx2-dimensional matrix symbolizing dependency graph,
     * and topologically sorts it
     *
     * @param roots list of
     * @param leaves
     * @return
     */
    static public <T> List<T> sort(T[][] nx2) throws TopoSortCycleException {
        int len = nx2.length;
        TopoSort<T> ts = new TopoSort<T>();
        for (int i = 0; i < len; i++) {
            ts.addOrderedPair(nx2[i][0], nx2[i][1]);
        }

        return ts.sort();
    }

    /**
     *
     * @param root root object
     * @param leaf object or null
     */
    public void addOrderedPair(T root, T leaf) {

        // add dependent

        if (leaf != null) {
            Counter cnt = leaves.get(leaf);
            if (cnt == null) {
                cnt = new Counter();
                leaves.put(leaf, cnt);
            }

            ++cnt.cnt;
            roots.remove(leaf);
            // add dependency

            List<T> deps = dependents.get(root);
            if (deps == null) {
                deps = new ArrayList<T>();
                dependents.put(root, deps);
            }
            deps.add(leaf);

        }

        if (!roots.contains(root) && !leaves.containsKey(root)) {
            roots.add(root);
        }
    }

    public List<T> sort() throws TopoSortCycleException {
        ArrayList<T> res = new ArrayList<T>();
        while (true) {
            int roots_size = roots.size();
            if (roots_size == 0) {
                if (leaves.size() > 0) {
                    throw new TopoSortCycleException(leaves.keySet());
                }

                return res;
            }

            T root = roots.get(roots_size - 1);

            res.add(root);
            roots.remove(roots_size - 1);

            List<T> deps = dependents.get(root);

            if (deps != null) {
                for (Iterator<T> iter = deps.iterator(); iter.hasNext();) {
                    T dep = iter.next();
                    Counter cnt = leaves.get(dep);

                    if (--cnt.cnt == 0) {
                        leaves.remove(dep);
                        roots.add(dep);
                    }

                }
            }

        }

    }

}
