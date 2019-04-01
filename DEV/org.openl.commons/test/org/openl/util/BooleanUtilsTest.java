package org.openl.util;

import static org.junit.Assert.*;

import org.junit.Test;

public class BooleanUtilsTest {

    @Test
    public void testObjectToTrue() {
        assertTrue(BooleanUtils.toBooleanObject("y"));
        assertTrue(BooleanUtils.toBooleanObject("Y"));
        assertTrue(BooleanUtils.toBooleanObject("t"));
        assertTrue(BooleanUtils.toBooleanObject("T"));
        assertTrue(BooleanUtils.toBooleanObject("On"));
        assertTrue(BooleanUtils.toBooleanObject("on"));
        assertTrue(BooleanUtils.toBooleanObject("Yes"));
        assertTrue(BooleanUtils.toBooleanObject("yEs"));
        assertTrue(BooleanUtils.toBooleanObject("tRUe"));
        assertTrue(BooleanUtils.toBooleanObject("TruE"));
        assertTrue(BooleanUtils.toBooleanObject(1));
        assertTrue(BooleanUtils.toBooleanObject(987));
        assertTrue(BooleanUtils.toBooleanObject(-1));
        assertTrue(BooleanUtils.toBooleanObject(true));
    }

    @Test
    public void testObjectToFalse() {
        assertFalse(BooleanUtils.toBooleanObject("n"));
        assertFalse(BooleanUtils.toBooleanObject("N"));
        assertFalse(BooleanUtils.toBooleanObject("f"));
        assertFalse(BooleanUtils.toBooleanObject("F"));
        assertFalse(BooleanUtils.toBooleanObject("no"));
        assertFalse(BooleanUtils.toBooleanObject("nO"));
        assertFalse(BooleanUtils.toBooleanObject("ofF"));
        assertFalse(BooleanUtils.toBooleanObject("Off"));
        assertFalse(BooleanUtils.toBooleanObject("FalSE"));
        assertFalse(BooleanUtils.toBooleanObject("falSe"));
        assertFalse(BooleanUtils.toBooleanObject(0));
        assertFalse(BooleanUtils.toBooleanObject(false));
    }

    @Test
    public void testObjectToNull() {
        assertNull(BooleanUtils.toBooleanObject(null));
        assertNull(BooleanUtils.toBooleanObject(""));
        assertNull(BooleanUtils.toBooleanObject("Not"));
        assertNull(BooleanUtils.toBooleanObject("yet"));
        assertNull(BooleanUtils.toBooleanObject("fake"));
        assertNull(BooleanUtils.toBooleanObject(1.1));
        assertNull(BooleanUtils.toBooleanObject(1L));
    }

    @Test
    public void testNullToFalse() {
        assertFalse(BooleanUtils.toBoolean(null));
        assertFalse(BooleanUtils.toBoolean(""));
        assertFalse(BooleanUtils.toBoolean("Not"));
        assertFalse(BooleanUtils.toBoolean("yet"));
        assertFalse(BooleanUtils.toBoolean("fake"));
        assertFalse(BooleanUtils.toBoolean(1.1));
        assertFalse(BooleanUtils.toBoolean(1L));
    }
}
