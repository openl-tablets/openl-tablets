/*
 * Created on Nov 13, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * @author snshor
 *
 */
public class TopoSortTest extends TestCase {

    /**
     * Constructor for TopoSortTest.
     *
     * @param arg0
     */
    public TopoSortTest(String arg0) {
        super(arg0);
    }

    public void testArrays() {
        List<Integer> res;
        res = TopoSort.sort(new Integer[] { 7, 3, 5, 5 }, new Integer[] { 9, 5, 9, 7 });
        System.out.println("Arrays: " + res);
    }

    public void testCycles() {
        TopoSort<String> ts = new TopoSort<String>();
        ts.addOrderedPair("7", "9");
        ts.addOrderedPair("3", "5");
        ts.addOrderedPair("5", "9");
        ts.addOrderedPair("5", "7");

        ts.addOrderedPair("7", "5");

        List<String> alist;
        try {
            alist = ts.sort();
            for (Iterator<String> iter = alist.iterator(); iter.hasNext();) {
                System.out.println(iter.next());
            }
        } catch (TopoSortCycleException e) {
            System.err.println(e.getMessage());
        }

    }

    public void testMatrix() {
        List<Integer> res;
        res = TopoSort.sort(new Integer[][] { { 7, 9 }, { 3, 5 }, { 5, 9 }, { 5, 7 } });
        System.out.println("Matrix: " + res);
    }

}
