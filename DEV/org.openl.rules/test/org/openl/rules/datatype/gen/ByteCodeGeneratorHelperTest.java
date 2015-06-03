package org.openl.rules.datatype.gen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ByteCodeGeneratorHelperTest {
    
    @Test
    public void testGetJavaType() {
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class));
        assertEquals("[I", ByteCodeGeneratorHelper.getJavaType(int[].class));
        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class));
        assertEquals("[Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String[].class));
    }

    @Test
    public void testGetTypeWriter() {
        assertEquals("org.openl.rules.datatype.gen.types.writers.StringTypeWriter",
                ByteCodeGeneratorHelper.getTypeWriter(new DefaultFieldDescription(String.class)).getClass().getName());

        DefaultFieldDescription field = new DefaultFieldDescription(DriverTest.class);
        field.setDefaultValueAsString(FieldDescription.DEFAULT_KEY_WORD);
        assertEquals("org.openl.rules.datatype.gen.types.writers.DefaultConstructorTypeWriter",
                ByteCodeGeneratorHelper.getTypeWriter(field).getClass().getName());

    }



    private static class DriverTest {

    }

}
