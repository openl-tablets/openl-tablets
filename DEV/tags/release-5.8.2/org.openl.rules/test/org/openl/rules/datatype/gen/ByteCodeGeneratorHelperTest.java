package org.openl.rules.datatype.gen;

import static org.junit.Assert.*;

import org.junit.Test;

public class ByteCodeGeneratorHelperTest {
    
    @Test
    public void testGetJavaType() {
        assertEquals("I", ByteCodeGeneratorHelper.getJavaType(int.class));
        assertEquals("[I", ByteCodeGeneratorHelper.getJavaType(int[].class));
        assertEquals("Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String.class));
        assertEquals("[Ljava/lang/String;", ByteCodeGeneratorHelper.getJavaType(String[].class));
    }

}
