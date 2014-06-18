package org.openl.rules.datatype.gen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.datatype.gen.types.writers.StringTypeWriter;

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
                ByteCodeGeneratorHelper.getTypeWriter(new FieldDescription(String.class)).getClass().getName());

        FieldDescription field = new FieldDescription(DriverTest.class);
        field.setDefaultValueAsString(FieldDescription.DEFAULT_KEY_WORD);
        assertEquals("org.openl.rules.datatype.gen.types.writers.DefaultConstructorTypeWriter",
                ByteCodeGeneratorHelper.getTypeWriter(field).getClass().getName());

    }

    private static class DriverTest {

    }

}
