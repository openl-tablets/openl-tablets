/*
 * Created on May 12, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util;

import java.util.Map;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.openl.util.meta.IOrderMetaInfo;

/**
 * @author snshor
 */
public class SkipListTest extends TestCase {

    String[] data = { "aaa", "bbb", "ddd", "ccc" };

    String[] keys = { "x1", "x2", "x4", "x4" };

    String[] data2 = { null, null, null, "ddd" };

    /**
     * Constructor for SkipListTest.
     *
     * @param name
     */
    public SkipListTest(String name) {
        super(name);
    }

    SkipList createSkipList() {
        SkipList s = new SkipList(IOrderMetaInfo.DEFAULT_COMPARATOR, 0.3, 5);
        for (int i = 0; i < data.length; i++) {
            s.put(data[i], data[i]);
        }
        return s;
    }

    void mapBenchmarkEmpty(Map map) {

        int n = 1000000;

        int maxN = 50000;

        long start = System.currentTimeMillis();

        for (int i = 0; i < n; ++i) {
            // int jj = r.nextInt();
            int jj = i;

            jj = jj % maxN;

            // map.put(ii, ii);
        }

        long end = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (end - start) + "ms. Size: " + map.size());

    }

    void mapBenchmarkGet(Map map) {

        int n = 1000000;

        int maxN = 50000;

        long start = System.currentTimeMillis();

        for (int i = 0; i < n; ++i) {
            // int jj = r.nextInt();
            int jj = i;

            jj = jj % maxN;

            Integer ii = new Integer(jj);

            Assert.assertEquals(ii, map.get(ii));
        }

        long end = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (end - start) + "ms. Size: " + map.size());

    }

    void mapBenchmarkPut(Map map) {

        int n = 1000000;

        int maxN = 50000;

        long start = System.currentTimeMillis();

        for (int i = 0; i < n; ++i) {
            // int jj = r.nextInt();
            int jj = i;

            jj = jj % maxN;

            Integer ii = new Integer(jj);

            map.put(ii, ii);
        }

        long end = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (end - start) + "ms. Size: " + map.size());

    }

    void mapBenchmarkRemove(Map map) {

        int n = 1000000;

        int maxN = 50000;

        long start = System.currentTimeMillis();

        for (int i = 0; i < n; ++i) {
            // int jj = r.nextInt();
            int jj = i;

            jj = jj % maxN;

            Integer ii = new Integer(jj);

            map.remove(ii);
        }

        long end = System.currentTimeMillis();

        System.out.println("Elapsed time: " + (end - start) + "ms. Size: " + map.size());

    }

    public void testContainsKey() {
        SkipList s = createSkipList();
        for (int i = 0; i < data.length; i++) {
            Assert.assertTrue(s.containsKey(data[i]));
        }
    }

    public void testContainsValue() {
        SkipList s = createSkipList();
        for (int i = 0; i < data.length; i++) {
            Assert.assertTrue(s.containsValue(data[i]));
        }

    }

    public void testGet() {
        SkipList s = createSkipList();
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data[i], s.get(data[i]));
        }

    }

    public void testIsEmpty() {
        SkipList s = createSkipList();
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(false, s.isEmpty());
        }

    }

//    public void testPerformance() {
//        Map<Object, Object> m1 = new HashMap<Object, Object>(10000);
//        Map<Object, Object> m2 = new HashMap<Object, Object>();
//
//        Map<Object, Object> m3 = new SkipList();
//        Map m4 = new TreeMap();
//
//        mapBenchmarkPut(m3);
//        mapBenchmarkPut(m1);
//        mapBenchmarkPut(m2);
//        mapBenchmarkPut(m4);
//
//        System.out.println("====");
//
//        mapBenchmarkGet(m3);
//        mapBenchmarkGet(m1);
//        mapBenchmarkGet(m2);
//        mapBenchmarkGet(m4);
//
//        System.out.println("====");
//
//        // mapBenchmarkRemove(m3);
//        mapBenchmarkRemove(m1);
//        mapBenchmarkRemove(m2);
//        mapBenchmarkRemove(m4);
//
//        System.out.println("====");
//
//        mapBenchmarkEmpty(m4);
//
//    }

    public void testSize() {
        SkipList s = createSkipList();
        for (int i = 0; i < data.length; i++) {
            Assert.assertEquals(data.length, s.size());
        }
    }

    public void testSkipList2() {
        SkipList s = new SkipList(IOrderMetaInfo.DEFAULT_COMPARATOR, 0.3, 5);
        for (int i = 0; i < data.length; i++) {
            Object res = s.put(keys[i], data[i]);
            Assert.assertEquals(data2[i], res);
        }

    }

}
