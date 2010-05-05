package org.openl.rules.helpers;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilTest {
    
    @Test
    public void testContainsString() {
        String value1 = "value1";
        String value2 = "value2";
        String value3 = "value3";
        String[] stringArray = new String[]{value1, value2};
        
        assertTrue(Util.contains(stringArray, value1));
        assertFalse(Util.contains(stringArray, value3));        
        
        String value4 = "value4";        
        String[] stringArray2 = new String[]{value1,value2, value3, value4};
        
        assertTrue(Util.contains(stringArray2, stringArray));
        
    }
    
    @Test
    public void testContainsInt() {
        int value1 = 1;
        int value2 = 2;
        int value3 = 3;
        
        int[] intArray = new int[]{value1, value2, value3};
        
        assertTrue(Util.contains(intArray, value2));        
        assertFalse(Util.contains(intArray, 15));
        
        int value4 = 4;
        int[] intArray2 = new int[]{value1, value2, value3, value4};
        
        assertTrue(Util.contains(intArray2, intArray));
        assertFalse(Util.contains(intArray2, null));
        assertFalse(Util.contains(intArray, intArray2));
        
    }
    
    @Test
    public void testContainsDouble() {
        double value1 = 1.567;
        double value2 = 2.567;
        double value3 = 3.567;
        
        double[] intArray = new double[]{value1, value2, value3};
        
        assertTrue(Util.contains(intArray, value2));        
        assertFalse(Util.contains(intArray, 15.666));
        
        double value4 = 4.789;
        double[] doubleArray2 = new double[]{value1, value2, value3, value4};
        
        assertTrue(Util.contains(doubleArray2, intArray));
        assertFalse(Util.contains(doubleArray2, null));
        assertFalse(Util.contains(intArray, doubleArray2));
    }
    
    @Test
    public void testContainsBoolean() {
        boolean[] booleanArray = new boolean[]{false, false};
        
        assertTrue(Util.contains(booleanArray, false));        
        assertFalse(Util.contains(booleanArray, true));
        
        boolean[] booleanArray2 = new boolean[]{false, false, true};
        
        assertTrue(Util.contains(booleanArray2, booleanArray));
        assertFalse(Util.contains(booleanArray2, null));
        assertFalse(Util.contains(booleanArray, booleanArray2));
    }

}
