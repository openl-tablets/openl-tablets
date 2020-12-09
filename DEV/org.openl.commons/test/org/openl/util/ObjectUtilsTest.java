package org.openl.util;

import java.io.File;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

public class ObjectUtilsTest {

    @Test
    public void convertTest() {
        Assert.assertNull(ObjectUtils.convert(null, Object.class));
        Assert.assertNull(ObjectUtils.convert(null, String.class));
        Assert.assertNull(ObjectUtils.convert(null, Double.class));
        Assert.assertEquals("test", ObjectUtils.convert("test", String.class));
        Assert.assertEquals(122.0, ObjectUtils.convert("122", Double.class));
        Assert.assertEquals(11.3, ObjectUtils.convert("11.3", Double.class));
        Assert.assertEquals(31, ObjectUtils.convert("31", int.class));
        Assert.assertEquals(57L, ObjectUtils.convert("57", long.class));
        Assert.assertEquals(false, ObjectUtils.convert("57", Boolean.class));
        Assert.assertEquals(true, ObjectUtils.convert("true", Boolean.class));
        Assert.assertEquals(BigInteger.valueOf(12365), ObjectUtils.convert("12365", BigInteger.class));
        Assert.assertEquals(RoundingMode.UP, ObjectUtils.convert("UP", RoundingMode.class));
        Assert.assertEquals(LocalDateTime.parse("2020-07-12T12:24:59"), ObjectUtils.convert("2020-07-12T12:24:59", LocalDateTime.class));
        Assert.assertEquals(new File("."), ObjectUtils.convert(".", File.class));
        Assert.assertArrayEquals(new int[]{1, 2, 3, 4, 5}, (int[]) ObjectUtils.convert("1,2,3,4,5", int[].class));
        Assert.assertArrayEquals(new Double[]{1.1d, 2.2d, 3.3d, 4.4d, 5d}, (Double[]) ObjectUtils.convert("1.1,2.2,3.3,4.4,5", Double[].class));
        Assert.assertArrayEquals(new String[]{"foo", " bar  "}, (String[]) ObjectUtils.convert("foo, bar  ", String[].class));
    }
}
