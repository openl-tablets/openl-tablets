package org.openl.rules.datatype.gen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.rules.datatype.gen.bean.writers.DefaultValue;
import org.openl.util.StringUtils;

public class ByteCodeGeneratorHelperTest {
    
    @Test
    public void testGetTypeWriter() {
        assertEquals("org.openl.rules.datatype.gen.types.writers.StringTypeWriter",
                ByteCodeGeneratorHelper.getTypeWriter(new DefaultFieldDescription(String.class)).getClass().getName());

        DefaultFieldDescription field = new DefaultFieldDescription(DriverTest.class);
        field.setDefaultValueAsString(DefaultValue.DEFAULT);
        assertEquals("org.openl.rules.datatype.gen.types.writers.DefaultConstructorTypeWriter",
                ByteCodeGeneratorHelper.getTypeWriter(field).getClass().getName());

    }

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



    private static class DriverTest {

    }

}
