package org.openl.util.math;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import org.openl.util.BooleanUtils;

/**
 * @author DLiauchuk
 * TODO: test all methods.
 */
public class MathUtilsTest {
    @Test
    public void testAvgByte() {
        byte[] byteArray = getTestByteArray();
        assertEquals((byte)5, MathUtils.avg(byteArray));   
        
        byte[] nullArray = null;
        assertEquals((byte)0, MathUtils.avg(nullArray));
        
        byte[] emptyArray = new byte[1];
        assertEquals((byte)0, MathUtils.avg(emptyArray));
    }    
    
    @Test
    public void testAvgShort() {
        short[] shortArray = new short[]{2, 3, 4};        
        assertEquals((short)3, MathUtils.avg(shortArray));   
        
        short[] nullArray = null;
        assertEquals((short)0, MathUtils.avg(nullArray));
        
        short[] emptyArray = new short[1];
        assertEquals((short)0, MathUtils.avg(emptyArray));
    }
    
    @Test
    public void testAvgBigInteger() {
        BigInteger[] bigIntegerArray = new BigInteger[]{BigInteger.valueOf(2), BigInteger.valueOf(3), BigInteger.valueOf(4)};        
        assertEquals(BigInteger.valueOf(3), MathUtils.avg(bigIntegerArray));   
        
        BigInteger[] nullArray = null;
        assertEquals(BigInteger.valueOf(0), MathUtils.avg(nullArray));
        
        BigInteger[] emptyArray = new BigInteger[1];
        assertEquals(BigInteger.valueOf(0), MathUtils.avg(emptyArray));
    }
    
    @Test
    public void testMedianByte() {
        byte[] byteArray = getTestByteArray();
        assertEquals(5, MathUtils.median(byteArray));
    }    
    
    @Test
    public void testMedianFloat() {
        float[] floatArray = new float[]{(float)10.5, (float)7.2};        
        assertEquals((float)8.85, (float)MathUtils.median(floatArray), 0.01);
    }
    
    @Test 
    public void testProductByte() {
        byte[] byteArray = new byte[]{3, 4};        
        assertEquals(12, MathUtils.product(byteArray), 0.01);
    }
    
    @Test 
    public void testProductDouble() {
        double[] doubleArray = new double[]{3, 4, 4.5, -6.78};        
        assertEquals(-366.12, MathUtils.product(doubleArray), 0.01);
    }
    
    @Test
    public void testQuaotientDouble() {
        assertEquals(1, MathUtils.quaotient(3.22, 1.75));
    }
    
    @Test
    public void testQuaotientBigDecimal() {
        assertEquals(1, MathUtils.quaotient(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        
        BigDecimal nullObj = null;
        assertEquals(0, MathUtils.quaotient(nullObj, nullObj));
        
        try {
            assertEquals(0, MathUtils.quaotient(BigDecimal.valueOf(3.22), BigDecimal.valueOf(0)));
            fail();
        } catch (ArithmeticException e) {
            assertTrue(true);
        }        
    }
    
    @Test 
    public void testModByte() {        
        assertEquals(6, MathUtils.mod((byte)19, (byte)13));
        assertEquals(-7, MathUtils.mod((byte)19, (byte)-13));
        assertEquals(7, MathUtils.mod((byte)-19, (byte)13));
        assertEquals(-6, MathUtils.mod((byte)-19, (byte)-13));
        assertEquals(0, MathUtils.mod((byte)19, (byte)19));
        assertEquals(0, MathUtils.mod((byte)0, (byte)19));
    }
    
    @Test 
    public void testModFloat() {   
        assertEquals(1.47, MathUtils.mod((float)3.22, (float)1.75), 0.01);        
    }
    
    @Test 
    public void testModDouble() {   
        assertEquals(1.47, MathUtils.mod(3.22, 1.75), 0.01);
    }
    
    @Test 
    public void testModBigDecimal() {   
        assertEquals(BigDecimal.valueOf(1.47), MathUtils.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(0), MathUtils.mod(null, BigDecimal.valueOf(1.75)));
    }
    
    @Test 
    public void testSmallInt() {
        int[] intMas = new int[]{10, 17, 13, 44, 1};
        assertEquals(1, MathUtils.small(intMas, 1));
        assertEquals(10, MathUtils.small(intMas, 2));
        assertEquals(13, MathUtils.small(intMas, 3));
        assertEquals(44, MathUtils.small(intMas, 5));
        
        intMas = null;
        assertEquals(0, MathUtils.small(intMas, 5));
        
        intMas = new int[1];
        assertEquals(0, MathUtils.small(intMas, 5));        
    }
    
    @Test 
    public void testAnd() {
        boolean[] boolAr = null;
        assertFalse(BooleanUtils.and(boolAr));
        
        boolAr = new boolean[]{true, false, true};        
        assertFalse(BooleanUtils.and(boolAr));
        
        boolAr = new boolean[]{true, true, true}; 
        assertTrue(BooleanUtils.and(boolAr));
    }
    
    private byte[] getTestByteArray() {
        return new byte[] {3, 5, 8};        
    }
}
