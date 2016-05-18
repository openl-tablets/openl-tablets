package org.openl.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.openl.util.CollectionUtils.Predicate;

/**
 * Created by tsaltsevich on 5/6/2016.
 */
public class CollectionUtilsTest {
    private static final Predicate<Integer> isEven = new Predicate<Integer>() {
        @Override
        public boolean evaluate(Integer number) {
            return (number % 2) == 0;
        }
    };

    @Test
    public void testIsEmptyCollection() throws Exception {
        List lnkLst = new LinkedList();
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(lnkLst));
        lnkLst.add("");
        assertFalse("Collection is empty", CollectionUtils.isEmpty(lnkLst));
        lnkLst.add("element");
        assertFalse("Collection is empty", CollectionUtils.isEmpty(lnkLst));
        lnkLst = null;
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(lnkLst));
    }

    @Test
    public void testIsNotEmptyCollection() throws Exception {
        List lnkLst = new LinkedList();
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(lnkLst));
        lnkLst.add("");
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(lnkLst));
        lnkLst.add("element");
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(lnkLst));
        lnkLst = null;
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(lnkLst));
    }

    @Test
    public void testIsEmptyMap() throws Exception {
        Map map = new HashMap();
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(map));
        map.put(null, null);
        assertFalse("Collection is empty", CollectionUtils.isEmpty(map));
        map.put("A", 1);
        assertFalse("Collection is empty", CollectionUtils.isEmpty(map));
        map = null;
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(map));
    }

    @Test
    public void testIsNotEmptyMap() throws Exception {
        Map map = new HashMap();
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(map));
        map.put(null, null);
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(map));
        map.put("A", 1);
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(map));
        map = null;
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(map));
    }

    @Test
    public void testIsEmptyObjectArray() throws Exception {
        Object[] array = null;
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(array));
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(new Object[] {}));
        assertFalse("Collection is empty", CollectionUtils.isEmpty(new Object[] { null }));
        assertFalse("Collection is empty", CollectionUtils.isEmpty(new Object[] { 1, "ABc", 'e' }));
    }

    @Test
    public void testIsNotEmptyObjectArray() throws Exception {
        Object[] array = null;
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(array));
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(new Object[] {}));
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(new Object[] { null }));
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(new Object[] { 1, "ABc", 'e' }));
    }

    @Test
    public void testIsEmptyPrimitiveArray() throws Exception {
        assertTrue("Array is not empty", CollectionUtils.isEmpty((int[]) null));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new int[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new double[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new long[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new short[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new byte[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new char[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new float[] {}));
        assertTrue("Array is not empty", CollectionUtils.isEmpty(new boolean[] {}));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new int[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new double[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new long[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new short[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new byte[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new char[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new float[] { 0 }));
        assertFalse("Array is empty", CollectionUtils.isEmpty(new boolean[] { false }));
    }

    @Test
    public void testIsNotEmptyPrimitiveObject() throws Exception {
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty((int[]) null));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new int[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new double[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new long[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new short[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new byte[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new char[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new float[] {}));
        assertFalse("Array is not empty", CollectionUtils.isNotEmpty(new boolean[] {}));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new int[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new double[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new long[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new short[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new byte[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new char[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new float[] { 0 }));
        assertTrue("Array is empty", CollectionUtils.isNotEmpty(new boolean[] { false }));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsEmptyNotArray() {
        CollectionUtils.isEmpty(0);
        fail("IllegalArgumentException is expected but not appeared");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIsNotEmptyNotArray() {
        CollectionUtils.isNotEmpty(0);
        fail("IllegalArgumentException is expected but not appeared");
    }

    @Test
    public void testMap() throws Exception {
        CollectionUtils.Mapper mapper = new CollectionUtils.Mapper<Integer, String>() {
            @Override
            public String map(Integer i) {
                return i.toString();
            }
        };
        assertNull("Collection is not null", CollectionUtils.map(null, mapper));
        assertArrayEquals("Returned collection is not correct",
            new Object[] { "1", "2", "3" },
            CollectionUtils.map(Arrays.asList(1, 2, 3), mapper).toArray());
        assertArrayEquals("Returned collection is not correct",
            new Object[] {},
            CollectionUtils.map(Arrays.asList(), mapper).toArray());
        assertArrayEquals("Returned collection is not correct",
            new Object[] { "0" },
            CollectionUtils.map(Arrays.asList(0), mapper).toArray());
    }

    @Test(expected = NullPointerException.class)
    public void testNullMapperMap() {
        CollectionUtils.map(Arrays.asList(1, 2, 3), null);
        fail("NullPointerException is expected but not appeared");
    }

    @Test
    public void testFindFirst() throws Exception {
        List<Integer> numbers = null;
        assertNull("Collection is not null", CollectionUtils.findFirst(numbers, isEven));
        assertNull("Wrong element is returned", CollectionUtils.findFirst(Arrays.asList(1, 3, 5, 9), isEven));
        assertEquals("Wrong element is returned",
            (Integer) 2,
            CollectionUtils.findFirst(Arrays.asList(1, 2, 3, 4, 5, 10), isEven));
        assertEquals("Wrong element is returned",
            (Integer) 10,
            CollectionUtils.findFirst(Arrays.asList(1, 3, 5, 10), isEven));
    }

    @Test(expected = NullPointerException.class)
    public void testNullPredicateFindFirst() {
        CollectionUtils.findFirst(Arrays.asList(2, 0, 6, 10), null);
        fail("NullPointerException is expected but not appeared");
    }

    @Test
    public void testFindAll() throws Exception {
        List<Integer> numbers = null;
        assertNull("Wrong element is returned", CollectionUtils.findAll(numbers, isEven));
        assertArrayEquals("Returned collection is not correct",
            new Object[] {},
            CollectionUtils.findAll(Arrays.asList(1, 3, 5, 9), isEven).toArray());
        assertArrayEquals("Returned collection is not correct",
            new Object[] { 2, 4, 10 },
            CollectionUtils.findAll(Arrays.asList(1, 2, 3, 4, 5, 10), isEven).toArray());
        assertArrayEquals("Returned collection is not correct",
            new Object[] { 0 },
            CollectionUtils.findAll(Arrays.asList(0), isEven).toArray());
    }

    @Test(expected = NullPointerException.class)
    public void testNullPredicateFindAll() {
        CollectionUtils.findAll(Arrays.asList(2, 0, 6, 10), null);
        fail("NullPointerException is expected but not appeared");
    }

    @Test
    public void testHasNull() throws Exception {
        assertFalse("Collection has null", CollectionUtils.hasNull(new Object[] {}));
        assertFalse("Collection has null", CollectionUtils.hasNull(new Object[] { 1, "null", 'e' }));
        assertTrue("Collection has not null", CollectionUtils.hasNull(new Object[] { null }));
        assertTrue("Collection has not null", CollectionUtils.hasNull(new Object[] { 1, 'e', null }));
    }

    @Test(expected = NullPointerException.class)
    public void testNullInputHasNull() {
        CollectionUtils.hasNull(null);
        fail("NullPointerException is expected but not appeared");
    }

}