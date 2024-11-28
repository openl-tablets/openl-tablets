package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.openl.util.CollectionUtils.Predicate;

/**
 * @author tsaltsevich, Yury Molchan
 */
public class CollectionUtilsTest {
    private static final Predicate<Integer> isEven = number -> number % 2 == 0;

    @Test
    public void testIsEmptyCollection() {
        List<String> list = new ArrayList<>();
        assertTrue(CollectionUtils.isEmpty(list), "Collection is not empty");
        list.add("");
        assertFalse(CollectionUtils.isEmpty(list), "Collection is empty");
        list.add("element");
        assertFalse(CollectionUtils.isEmpty(list), "Collection is empty");
        assertTrue(CollectionUtils.isEmpty((List) null), "Collection is not empty");
    }

    @Test
    public void testIsNotEmptyCollection() {
        List<String> list = new ArrayList<>();
        assertFalse(CollectionUtils.isNotEmpty(list), "Collection is empty");
        list.add("");
        assertTrue(CollectionUtils.isNotEmpty(list), "Collection is not empty");
        list.add("element");
        assertTrue(CollectionUtils.isNotEmpty(list), "Collection is not empty");
        assertFalse(CollectionUtils.isNotEmpty((List) null), "Collection is empty");
    }

    @Test
    public void testIsEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        assertTrue(CollectionUtils.isEmpty(map), "Collection is not empty");
        map.put(null, null);
        assertFalse(CollectionUtils.isEmpty(map), "Collection is empty");
        map.put("A", 1);
        assertFalse(CollectionUtils.isEmpty(map), "Collection is empty");
        assertTrue(CollectionUtils.isEmpty((Map) null), "Collection is not empty");
    }

    @Test
    public void testIsNotEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        assertFalse(CollectionUtils.isNotEmpty(map), "Collection is empty");
        map.put(null, null);
        assertTrue(CollectionUtils.isNotEmpty(map), "Collection is not empty");
        map.put("A", 1);
        assertTrue(CollectionUtils.isNotEmpty(map), "Collection is not empty");
        assertFalse(CollectionUtils.isNotEmpty((Map) null), "Collection is empty");
    }

    @Test
    public void testIsEmptyObjectArray() {
        assertTrue(CollectionUtils.isEmpty((Object[]) null), "Collection is not empty");
        assertTrue(CollectionUtils.isEmpty(new Object[]{}), "Collection is not empty");
        assertFalse(CollectionUtils.isEmpty(new Object[]{null}), "Collection is empty");
        assertFalse(CollectionUtils.isEmpty(new Object[]{1, "ABc", 'e'}), "Collection is empty");
    }

    @Test
    public void testIsNotEmptyObjectArray() {
        assertFalse(CollectionUtils.isNotEmpty((Object[]) null), "Collection is empty");
        assertFalse(CollectionUtils.isNotEmpty(new Object[]{}), "Collection is empty");
        assertTrue(CollectionUtils.isNotEmpty(new Object[]{null}), "Collection is not empty");
        assertTrue(CollectionUtils.isNotEmpty(new Object[]{1, "ABc", 'e'}), "Collection is not empty");
    }

    @Test
    public void testIsEmptyPrimitiveArray() {
        assertTrue(CollectionUtils.isEmpty((int[]) null), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new int[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new double[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new long[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new short[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new byte[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new char[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new float[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isEmpty(new boolean[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isEmpty(new int[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new double[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new long[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new short[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new byte[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new char[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new float[]{0}), "Array is empty");
        assertFalse(CollectionUtils.isEmpty(new boolean[]{false}), "Array is empty");
    }

    @Test
    public void testIsNotEmptyPrimitiveObject() {
        assertFalse(CollectionUtils.isNotEmpty((int[]) null), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new int[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new double[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new long[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new short[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new byte[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new char[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new float[]{}), "Array is not empty");
        assertFalse(CollectionUtils.isNotEmpty(new boolean[]{}), "Array is not empty");
        assertTrue(CollectionUtils.isNotEmpty(new int[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new double[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new long[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new short[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new byte[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new char[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new float[]{0}), "Array is empty");
        assertTrue(CollectionUtils.isNotEmpty(new boolean[]{false}), "Array is empty");
    }

    @Test
    public void testIsEmptyNotArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            CollectionUtils.isEmpty(0);
            fail("IllegalArgumentException is expected but not appeared");
        });
    }

    @Test
    public void testIsNotEmptyNotArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            CollectionUtils.isNotEmpty(0);
            fail("IllegalArgumentException is expected but not appeared");
        });
    }

    @Test
    public void testMap() {
        CollectionUtils.Mapper<Integer, String> mapper = Object::toString;
        assertNull(CollectionUtils.map(null, mapper), "Collection is not null");
        assertArrayEquals(new String[]{"1", "2", "3"},
                CollectionUtils.map(Arrays.asList(1, 2, 3), mapper).toArray(),
                "Returned collection is not correct");
        assertArrayEquals(new String[]{},
                CollectionUtils.map(new ArrayList<>(), mapper).toArray(),
                "Returned collection is not correct");
        assertArrayEquals(new String[]{"0"},
                CollectionUtils.map(Collections.singletonList(0), mapper).toArray(),
                "Returned collection is not correct");
    }

    @Test
    public void testNullMapperMap() {
        assertThrows(NullPointerException.class, () -> {
            CollectionUtils.map(Arrays.asList(1, 2, 3), null);
            fail("NullPointerException is expected but not appeared");
        });
    }

    @Test
    public void testFindFirst() {
        assertNull(CollectionUtils.findFirst(null, isEven), "Collection is not null");
        assertNull(CollectionUtils.findFirst(Arrays.asList(1, 3, 5, 9), isEven), "Wrong element is returned");
        assertEquals((Integer) 2,
                CollectionUtils.findFirst(Arrays.asList(1, 2, 3, 4, 5, 10), isEven),
                "Wrong element is returned");
        assertEquals((Integer) 10,
                CollectionUtils.findFirst(Arrays.asList(1, 3, 5, 10), isEven),
                "Wrong element is returned");
    }

    @Test
    public void testNullPredicateFindFirst() {
        assertThrows(NullPointerException.class, () -> {
            CollectionUtils.findFirst(Arrays.asList(2, 0, 6, 10), null);
            fail("NullPointerException is expected but not appeared");
        });
    }

    @Test
    public void testFindAll() {
        assertNull(CollectionUtils.findAll(null, isEven), "Wrong element is returned");
        assertArrayEquals(new Object[]{},
                CollectionUtils.findAll(Arrays.asList(1, 3, 5, 9), isEven).toArray(),
                "Returned collection is not correct");
        assertArrayEquals(new Object[]{2, 4, 10},
                CollectionUtils.findAll(Arrays.asList(1, 2, 3, 4, 5, 10), isEven).toArray(),
                "Returned collection is not correct");
        assertArrayEquals(new Object[]{0},
                CollectionUtils.findAll(Collections.singletonList(0), isEven).toArray(),
                "Returned collection is not correct");
    }

    @Test
    public void testNullPredicateFindAll() {
        assertThrows(NullPointerException.class, () -> {
            CollectionUtils.findAll(Arrays.asList(2, 0, 6, 10), null);
            fail("NullPointerException is expected but not appeared");
        });
    }

    @Test
    public void testHasNull() {
        assertFalse(CollectionUtils.hasNull(new Object[]{}), "Collection has null");
        assertFalse(CollectionUtils.hasNull(new Object[]{1, "null", 'e'}), "Collection has null");
        assertTrue(CollectionUtils.hasNull(new Object[]{null}), "Collection has not null");
        assertTrue(CollectionUtils.hasNull(new Object[]{1, 'e', null}), "Collection has not null");
    }

    @Test
    public void testNullInputHasNull() {
        assertThrows(NullPointerException.class, () -> {
            CollectionUtils.hasNull(null);
            fail("NullPointerException is expected but not appeared");
        });
    }

    @Test
    public void testToArray() {
        assertNull(CollectionUtils.toArray(null, Object.class));
        assertArrayEquals((int[]) CollectionUtils.toArray(Arrays.asList(3, 1, 2), int.class), new int[]{3, 1, 2});
        assertArrayEquals((Double[]) CollectionUtils.toArray(Arrays.asList(3.1, 1.2, 2.3), Double.class),
                new Double[]{3.1, 1.2, 2.3});
        assertArrayEquals((String[]) CollectionUtils.toArray(Collections.emptyList(), String.class), new String[0]);
        assertArrayEquals((byte[]) CollectionUtils.toArray(Collections.emptyList(), byte.class), new byte[0]);
    }
}
