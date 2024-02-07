package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

public class ObjectUtilsTest {

    @Test
    public void convertTest() {
        assertNull(ObjectUtils.convert(null, Object.class));
        assertNull(ObjectUtils.convert(null, String.class));
        assertNull(ObjectUtils.convert(null, Double.class));
        assertEquals("test", ObjectUtils.convert("test", String.class));
        assertEquals(122.0, ObjectUtils.convert("122", Double.class));
        assertEquals(11.3, ObjectUtils.convert("11.3", Double.class));
        assertEquals(31, ObjectUtils.convert("31", int.class));
        assertEquals(57L, ObjectUtils.convert("57", long.class));
        assertEquals(false, ObjectUtils.convert("57", Boolean.class));
        assertEquals(true, ObjectUtils.convert("true", Boolean.class));
        assertEquals(BigInteger.valueOf(12365), ObjectUtils.convert("12365", BigInteger.class));
        assertEquals(RoundingMode.UP, ObjectUtils.convert("UP", RoundingMode.class));
        assertEquals(LocalDateTime.parse("2020-07-12T12:24:59"), ObjectUtils.convert("2020-07-12T12:24:59", LocalDateTime.class));
        assertEquals(new File("."), ObjectUtils.convert(".", File.class));
        assertArrayEquals(new int[]{1, 2, 3, 4, 5}, (int[]) ObjectUtils.convert("1,2,3,4,5", int[].class));
        assertArrayEquals(new Double[]{1.1d, 2.2d, 3.3d, 4.4d, 5d}, (Double[]) ObjectUtils.convert("1.1,2.2,3.3,4.4,5", Double[].class));
        assertArrayEquals(new String[]{"foo", " bar  "}, (String[]) ObjectUtils.convert("foo, bar  ", String[].class));
    }
}
