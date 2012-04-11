package org.openl.util.generation;

import static org.junit.Assert.*;

import org.junit.Test;

public class JavaClassGeneratorHelperTest {
    
    @Test
    public void testCleanTypeName() {
        String str = JavaClassGeneratorHelper.cleanTypeName(null);
        assertNull(str);
        
        String str1 = JavaClassGeneratorHelper.cleanTypeName("org.openl.java.Driver[]");
        assertEquals("org.openl.java.Driver", str1);
        
        String str2 = JavaClassGeneratorHelper.cleanTypeName("org.openl.java.Driver");
        assertEquals("org.openl.java.Driver", str2);
        
        String str3 = JavaClassGeneratorHelper.cleanTypeName("[]");
        assertEquals("", str3);
        
        String str4 = JavaClassGeneratorHelper.cleanTypeName("org.openl.java.Driver[][]");
        assertEquals("org.openl.java.Driver", str4);
    }
    
    @Test
    public void testGetJavaArrayType() {
        String res = JavaClassGeneratorHelper.getJavaArrayType(null);
        assertNull(res);
        
        String res1 = JavaClassGeneratorHelper.getJavaArrayType("org.test.MyType[][]");
        assertEquals("[[Lorg/test/MyType;", res1);
        
        String res2 = JavaClassGeneratorHelper.getJavaArrayType("org.test.MyType");
        assertEquals("Lorg/test/MyType;", res2);
    }
    
    @Test
    public void testDimension() {
        assertEquals(-1, JavaClassGeneratorHelper.getDimension(null));
        
        assertEquals(2, JavaClassGeneratorHelper.getDimension("org.test.my.Hello[][]"));
        
        assertEquals(0, JavaClassGeneratorHelper.getDimension("org.test.my.Hello"));
        
        assertEquals(1, JavaClassGeneratorHelper.getDimension("org.test.my.Hello[]"));
    }
    
    
    

}
