package org.openl.rules.helpers;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.ByteValue;
import org.openl.meta.LongValue;
import org.openl.meta.number.NumberValue;
import org.openl.rules.TestHelper;

/**
 * Test to check that methods from {@link RulesUtils} and children of {@link NumberValue} are visible and executed from excel.
 * 
 * @author DLiauchuk
 * 
 * TODO: test all methods
 */
public class RulesUtilsTest {
    
    private static final String src = "test/rules/helpers/RulesUtilsTest.xlsx";
    
    private static TestInterf instance;
    
    public interface TestInterf {
        String testMaxByte(byte[] obj);
        BigInteger testSum(BigInteger[] values);
        LongValue testQuaotient(ByteValue number, ByteValue divisor);
        ByteValue testMin(ByteValue[] values);
        BigDecimalValue testAvg(BigDecimalValue[] values);
        boolean checkOr();
    }
    
    @Before
    public void init() {
        if (instance == null) {
            File xlsFile = new File(src);
            TestHelper<TestInterf> testHelper;
            testHelper = new TestHelper<TestInterf>(xlsFile, TestInterf.class);
            
            instance = testHelper.getInstance();    
        }  
    }
    
    @Test
    public void testByteMax() {
        assertEquals("res2", instance.testMaxByte(new byte[]{(byte)3, (byte)5}));
    }
    
    @Test
    public void testBigIntegerSum() {
        assertEquals(BigInteger.valueOf(30), instance.testSum(new BigInteger[]{BigInteger.valueOf(10), 
                BigInteger.valueOf(5), BigInteger.valueOf(15)}));
    }
    
    @Test
    public void testByteValueQuaotient() {
        assertEquals(new LongValue(2), instance.testQuaotient(new ByteValue((byte) 25), new ByteValue((byte) 12)));
    }
    
    @Test
    public void testByteValueMin() {
        assertEquals(new ByteValue((byte) 1), instance.testMin(new ByteValue[]{new ByteValue("10"), new ByteValue("15"), 
                new ByteValue("1")}));
    }
    
    @Test
    public void testBigDecimalValueAvg() {
        assertEquals(new BigDecimalValue("12.66667"), 
            instance.testAvg(new BigDecimalValue[]{
                    new BigDecimalValue("10"), new BigDecimalValue("15"), new BigDecimalValue("13")})); 
                
    }
    
    @Test
    public void testOrCallingFromRules() {
        assertTrue(instance.checkOr());
    }
    
    @Test
    public void testXOR3arguments() {
        assertFalse(callXor(false, false, false));
        
        assertTrue(callXor(true, false, false));
        
        assertTrue(callXor(false, true, false));
        
        assertFalse(callXor(true, true, false));

        assertTrue(callXor(false, false, true));
                
        assertFalse(callXor(true, false, true));
        
        assertFalse(callXor(false, true, true));
                
        assertTrue(callXor(true, true, true));
    }
    
    @Test
    public void testXOR2arguments() {
        assertFalse(callXor(false, false));

        assertTrue(callXor(false, true));

        assertTrue(callXor(true, false));

        assertFalse(callXor(true, true));
    }
    
    @Test
    public void testOR2arguments() {
        assertFalse(callOr(false, false));

        assertTrue(callOr(false, true));

        assertTrue(callOr(true, false));

        assertTrue(callOr(true, true));
    }
    
    @Test
    public void testOR3arguments() {
        assertFalse(callOr(false, false, false));
        
        assertTrue(callOr(true, false, false));
        
        assertTrue(callOr(false, true, false));
        
        assertTrue(callOr(true, true, false));

        assertTrue(callOr(false, false, true));
                
        assertTrue(callOr(true, false, true));
        
        assertTrue(callOr(false, true, true));
                
        assertTrue(callOr(true, true, true));
    }
    
    
    @Test
    public void testRoundToLong() {        
        assertEquals(1, RulesUtils.round(1.222235345345));
        
        assertEquals(2, RulesUtils.round(1.500000001235345345));
        
        assertEquals(0, RulesUtils.round(0));        
    }
    
    @Test
    public void testRoundWithPrecision() {        
        assertEquals("1.222", String.valueOf(RulesUtils.round(1.222235345345, 3)));
        
        assertEquals("1.6", String.valueOf(RulesUtils.round(1.56000001235345345, 1)));
        
        assertEquals("0.0", String.valueOf(RulesUtils.round(0, 0)));        
    }
    
    
    private boolean callXor(boolean... values) {
        return RulesUtils.xor(values);
    }
    
    private boolean callOr(boolean... values) {
        return RulesUtils.anyTrue(values);
    }
    
}
    