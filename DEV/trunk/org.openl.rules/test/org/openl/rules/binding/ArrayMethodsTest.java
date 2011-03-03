package org.openl.rules.binding;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.openl.rules.TestHelper;

public class ArrayMethodsTest {
    
    private static final String src = "test/rules/binding/ArrayMethodsTest.xlsx";
    
    private static ArrayMethodsInterf instance;
    
    public interface ArrayMethodsInterf {               
        int start();  
        int[] start1();
        int intTest(int a);
        int[] intArrayTest(int[] b);
        String[] personsNames();
        String[] personNamesFromArray();
        int[] test2MethodCalls();
    }
    
    @Before
    public void init() {
        if (instance == null) {
            File xlsFile = new File(src);
            TestHelper<ArrayMethodsInterf> testHelper;
            testHelper = new TestHelper<ArrayMethodsInterf>(xlsFile, ArrayMethodsInterf.class);
            
            instance = testHelper.getInstance();    
        }  
    }
    
    @Test
    public void testSingleCall() {
        assertEquals(25, instance.start());
    }
    
    @Test
    public void testArrayCall() {
        assertEquals(45, instance.start1()[1]);
    }
    
    @Test
    public void testInt() {
        assertEquals(5, instance.intTest(4));
        assertEquals(7, instance.intArrayTest(new int[]{4,5,6})[2]);
    }
    
    @Test
    public void testAccessFieldsMethodsForDataTable() {
        String[] names = instance.personsNames();
        assertTrue(names.length == 2);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }
    
    @Test
    public void testAccessFieldsMethodsForArrays() {
        String[] names = instance.personNamesFromArray();
        assertTrue(names.length == 2);
        assertEquals("Vasia", names[0]);
        assertEquals("Petia", names[1]);
    }
    
    @Test
    public void test2MethodsCall() {
        int[] a = instance.test2MethodCalls();
        assertTrue(a.length == 2);
        assertEquals(7, a[0]);
        assertEquals(9, a[1]);
    }
    

}
