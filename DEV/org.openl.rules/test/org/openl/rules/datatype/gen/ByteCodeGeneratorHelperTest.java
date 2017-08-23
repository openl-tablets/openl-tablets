package org.openl.rules.datatype.gen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.util.StringUtils;

public class ByteCodeGeneratorHelperTest {
    
    @Test
    public void testGetJavaType() {
        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class.getName()));
        assertEquals("[Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String[].class.getName()));
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class.getName()));
        assertEquals("[[I", ByteCodeGeneratorHelper.getJavaType(int[][].class.getName()));
        assertEquals("D", ByteCodeGeneratorHelper.getJavaType(double.class.getName()));
        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class.getName()));
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class.getName()));
        assertEquals("B", ByteCodeGeneratorHelper.getJavaType(byte.class.getName()));
        assertEquals("S", ByteCodeGeneratorHelper.getJavaType(short.class.getName()));
        assertEquals("J", ByteCodeGeneratorHelper.getJavaType(long.class.getName()));
        assertEquals("F", ByteCodeGeneratorHelper.getJavaType(float.class.getName()));
        assertEquals("D", ByteCodeGeneratorHelper.getJavaType(double.class.getName()));
        assertEquals("Z", ByteCodeGeneratorHelper.getJavaType(boolean.class.getName()));
        assertEquals("C", ByteCodeGeneratorHelper.getJavaType(char.class.getName()));
        assertEquals("[[Lorg/test/MyType;", ByteCodeGeneratorHelper.getJavaType("[[Lorg.test.MyType;"));
        assertEquals("Lorg/test/MyType;", ByteCodeGeneratorHelper.getJavaType("org.test.MyType"));
    }
}
