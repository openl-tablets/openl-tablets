package org.openl.rules.helpers;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigInteger;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.ByteValue;
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
        ByteValue testQuaotient(ByteValue number, ByteValue divisor);
        ByteValue testMin(ByteValue[] values);
        BigDecimalValue testAvg(BigDecimalValue[] values);
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
        assertEquals(new ByteValue((byte) 2), instance.testQuaotient(new ByteValue((byte) 25), new ByteValue((byte) 12)));
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
    
}
    