package org.openl.rules.datatype.gen;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DatatypeOpenField;
import org.openl.types.impl.DynamicArrayAggregateInfo;

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
