package org.openl.rules.datatype.gen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.util.StringUtils;

public class ByteCodeGeneratorHelperTest {
    
    @Test
    public void testGetJavaType() {
        assertEquals(StringUtils.EMPTY, ByteCodeGeneratorHelper.getJavaType((String) null));

        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class.getCanonicalName()));
        assertEquals("[Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String[].class.getCanonicalName()));
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class.getCanonicalName()));
        assertEquals("[[I", ByteCodeGeneratorHelper.getJavaType(int[][].class.getCanonicalName()));
        assertEquals("D", ByteCodeGeneratorHelper.getJavaType(double.class.getCanonicalName()));
        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class.getCanonicalName()));
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class.getCanonicalName()));
        assertEquals("B", ByteCodeGeneratorHelper.getJavaType(byte.class.getCanonicalName()));
        assertEquals("S", ByteCodeGeneratorHelper.getJavaType(short.class.getCanonicalName()));
        assertEquals("J", ByteCodeGeneratorHelper.getJavaType(long.class.getCanonicalName()));
        assertEquals("F", ByteCodeGeneratorHelper.getJavaType(float.class.getCanonicalName()));
        assertEquals("D", ByteCodeGeneratorHelper.getJavaType(double.class.getCanonicalName()));
        assertEquals("Z", ByteCodeGeneratorHelper.getJavaType(boolean.class.getCanonicalName()));
        assertEquals("C", ByteCodeGeneratorHelper.getJavaType(char.class.getCanonicalName()));
        assertEquals("[[Lorg/test/MyType;", ByteCodeGeneratorHelper.getJavaType("org.test.MyType[][]"));
        assertEquals("Lorg/test/MyType;", ByteCodeGeneratorHelper.getJavaType("org.test.MyType"));
    }
}
