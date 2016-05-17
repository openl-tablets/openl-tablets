package org.openl.util;

import org.junit.Test;

import java.util.*;

import org.openl.util.CollectionUtils.Predicate;

import static org.junit.Assert.*;

/**
 * Created by tsaltsevich on 5/6/2016.
 */
public class CollectionUtilsTest {
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
    public void testIsEmptyObject() throws Exception {
        Object[] array = null;
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(array));
        assertTrue("Collection is not empty", CollectionUtils.isEmpty(new Object[] {}));
        assertFalse("Collection is empty", CollectionUtils.isEmpty(new Object[] { null }));
        assertFalse("Collection is empty", CollectionUtils.isEmpty(new Object[] { 1, "ABc", 'e' }));
    }

    @Test
    public void testIsNotEmptyObject() throws Exception {
        Object[] array = null;
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(array));
        assertFalse("Collection is empty", CollectionUtils.isNotEmpty(new Object[] {}));
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(new Object[] { null }));
        assertTrue("Collection is not empty", CollectionUtils.isNotEmpty(new Object[] { 1, "ABc", 'e' }));
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

    private Predicate<Integer> isEven = new Predicate<Integer>() {
        @Override
        public boolean evaluate(Integer number) {
            return (number % 2) == 0;
        }
    };

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