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
    public void testGetJavaType() {
        assertEquals("Ljava/lang/String;", JavaClassGeneratorHelper.getJavaType(String.class.getCanonicalName()));
        assertEquals("[Ljava/lang/String;", JavaClassGeneratorHelper.getJavaType(String[].class.getCanonicalName()));
        assertEquals("I", JavaClassGeneratorHelper.getJavaType(int.class.getCanonicalName()));
        assertEquals("[[I", JavaClassGeneratorHelper.getJavaType(int[][].class.getCanonicalName()));
        assertEquals("D", JavaClassGeneratorHelper.getJavaType(double.class.getCanonicalName()));
    }
    
    @Test
    public void testGetJavaTypeWithPrefix() {
        assertEquals("Ljava/lang/String;", JavaClassGeneratorHelper.getJavaTypeWithPrefix(String.class.getCanonicalName()));
        assertEquals("I", JavaClassGeneratorHelper.getJavaTypeWithPrefix(int.class.getCanonicalName()));
        assertEquals("B", JavaClassGeneratorHelper.getJavaTypeWithPrefix(byte.class.getCanonicalName()));
        assertEquals("S", JavaClassGeneratorHelper.getJavaTypeWithPrefix(short.class.getCanonicalName()));
        assertEquals("J", JavaClassGeneratorHelper.getJavaTypeWithPrefix(long.class.getCanonicalName()));
        assertEquals("F", JavaClassGeneratorHelper.getJavaTypeWithPrefix(float.class.getCanonicalName()));
        assertEquals("D", JavaClassGeneratorHelper.getJavaTypeWithPrefix(double.class.getCanonicalName()));
        assertEquals("Z", JavaClassGeneratorHelper.getJavaTypeWithPrefix(boolean.class.getCanonicalName()));
        assertEquals("C", JavaClassGeneratorHelper.getJavaTypeWithPrefix(char.class.getCanonicalName()));
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
