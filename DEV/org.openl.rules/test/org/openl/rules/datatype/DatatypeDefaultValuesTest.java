package org.openl.rules.datatype;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Assert;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;

public class DatatypeDefaultValuesTest extends BaseOpenlBuilderHelper {
    
    private static final String src = "test/rules/datatype/DatatypeDefaultValues.xls";    

    public DatatypeDefaultValuesTest() {
        super(src);
    }
    
    @Before
    public void testBefore() {
        testNoErrors();
    }
    
    @SuppressWarnings("deprecation")
    private void testNoErrors() {
        Assert.assertTrue("No binding errors", getCompiledOpenClass().getBindingErrors().length == 0);
        Assert.assertTrue("No parsing errors", getCompiledOpenClass().getParsingErrors().length == 0);
        Assert.assertTrue("No warnings", getCompiledOpenClass().getMessages().isEmpty());        
    }
    
    @Test    
    public void testDefaultValues1() {
        Class<?> clazz = null;
        
        try {
            clazz = getClass("org.openl.generated.beans.TestType");
            checkTestTypeClass(clazz);           
        } catch (Throwable e) {            
            fail(e.getMessage());        
        }  
    }
    
    @Test    
    public void testDefaultValues2() {
        Class<?> clazz = null;
        try {
        	clazz = getClass("org.openl.generated.beans.TypeWithLong");
            checkTypeWithLong(clazz);
        } catch (Throwable e) {            
            fail(e.getMessage());        
        }  
    }
    
    @Test    
    public void testDefaultValues3() {
    	Class<?> clazz = null;
        
        try {
        	clazz = getClass("org.openl.generated.beans.TestType2");
            checkTestType2(clazz);
        } catch (Throwable e) {            
            fail(e.getMessage());        
        } 
    }
    
    @Test    
    public void testDefaultValues4() {
        Class<?> clazz = null;
        
        try {
        	clazz = getClass("org.openl.generated.beans.TestType3");
            checkTestType3(clazz);
        } catch (Throwable e) {            
            fail(e.getMessage());        
        } 
    }
    
    @Test    
    public void testDefaultValues5() {
        Class<?> clazz = null;
        
        try {
        	clazz = getClass("org.openl.generated.beans.TestBigTypes");
            checkTestBigTypes(clazz);
        } catch (Throwable e) {            
            fail(e.getMessage());        
        } 
    }
    
    @Test
    public void testDefaultValues6() {
        Class<?> clazz = null;
        
        try {
        	clazz = getClass("org.openl.generated.beans.TestOpenLGrammar");
        	checkTestOpenLGramar(clazz);
        } catch (Throwable e) {   
            e.printStackTrace();
            fail(e.getMessage());        
        } 
    }

    @Test
    public void testDefaultValue_Bean() {
        try {
            Class<?> clazz = getClass("org.openl.generated.beans.BeanType1");
            checkTestDefaultBean(clazz);
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void checkTestBigTypes(Class<?> clazz) throws InstantiationException,
                                                    IllegalAccessException,
                                                    NoSuchMethodException,
                                                    InvocationTargetException {
        Object instance = getInstance(clazz);
        
        String methodName = "getBigIntVal";
        testValue(clazz, instance, methodName, BigInteger.valueOf(2000000000));
        
        methodName = "getBigDecVal";
        testValue(clazz, instance, methodName, BigDecimal.valueOf(1115.37));
        
        methodName = "getBigIntVal2";
        testValue(clazz, instance, methodName, null);
        
        
    }

    private void checkTestType3(Class<?> clazz) throws InstantiationException,
                                                IllegalAccessException,
                                                NoSuchMethodException,
                                                InvocationTargetException {
        Object instance = getInstance(clazz);
        
        String methodName = "getDoubleValue";
        testValue(clazz, instance, methodName, new DoubleValue("12.44"));
        
        methodName = "getIntegerValue";
        testValue(clazz, instance, methodName, new Integer("45"));
        
        methodName = "getStr";
        testValue(clazz, instance, methodName, "fgfs");
        
    }    

    private void checkTestType2(Class<?> clazz) throws InstantiationException,
                                                IllegalAccessException,
                                                NoSuchMethodException,
                                                InvocationTargetException {
                
        Object instance = getInstance(clazz);
        
        String methodName = "getOpenlDouble";
        testValue(clazz, instance, methodName, new DoubleValue("12.23"));
    }

    private void checkTypeWithLong(Class<?> clazz) throws InstantiationException,
                                                    IllegalAccessException,
                                                    NoSuchMethodException,
                                                    InvocationTargetException {
        Object instance = getInstance(clazz);
        
        String methodName = "getStrVal";
        testValue(clazz, instance, methodName, null);            
        
        methodName = "getBoolVal";
        testValue(clazz, instance, methodName, true);
        
        methodName = "getDoubleVal";
        testValue(clazz, instance, methodName, new Double("45.47"));
        
        methodName = "getLongVal";
        testValue(clazz, instance, methodName, new Long("45678"));
    }

    private void checkTestTypeClass(Class<?> clazz) throws InstantiationException,
                                                   IllegalAccessException,
                                                   NoSuchMethodException,
                                                   InvocationTargetException {
        Object instance = getInstance(clazz);
        
        String methodName = "getName";
        testValue(clazz, instance, methodName, "Denis");
        
        methodName = "getByteVal";
        testValue(clazz, instance, methodName, new Byte((byte) 12));
        
        methodName = "getShortVal";
        testValue(clazz, instance, methodName, new Short((short)13));
        
        methodName = "getIntVal";
        testValue(clazz, instance, methodName, 14);
        
        methodName = "getCharVal";
        testValue(clazz, instance, methodName, 'c');
        
        methodName = "getFloatVal";
        testValue(clazz, instance, methodName, new Float("12.23"));
    }
    
	private void checkTestOpenLGramar(Class<?> clazz)
			throws InstantiationException, IllegalAccessException,
			NoSuchMethodException, InvocationTargetException {
		Object instance = getInstance(clazz);

		String methodName = "getFlag";
		testValue(clazz, instance, methodName, true);

		methodName = "getIntVal";
		testValue(clazz, instance, methodName, new Integer(1000));

		methodName = "getdVal";
		testValue(clazz, instance, methodName, new Double(1.26));
		
		methodName = "getStr";
		testValue(clazz, instance, methodName, "Hello World");
		
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		Date expectedDate = null;
		try {
            expectedDate = format.parse("01/01/2012");
        } catch (ParseException e) {
            e.printStackTrace();
        }
		        
		methodName = "getDateVal";
        testValue(clazz, instance, methodName, expectedDate);
	}

    private void testValue(Class<?> clazz, Object instance, String methodName, Object expectedResult) throws NoSuchMethodException,
                                                           IllegalAccessException,
                                                           InvocationTargetException {
        Method method = clazz.getMethod(methodName, new Class<?>[0]);
        assertNotNull(method);
        Object result = method.invoke(instance, new Object[0]);
        assertEquals(expectedResult, result);
    }

    private void checkTestDefaultBean(Class<?> clazz)
            throws InstantiationException, IllegalAccessException,
            NoSuchMethodException, InvocationTargetException, ClassNotFoundException {
        Object instance = getInstance(clazz);

        String methodName = "getName";
        testValue(clazz, instance, methodName, "Test name");

        methodName = "getSurname";
        testValue(clazz, instance, methodName, "Test surname");

        methodName = "getObj";
        Class<?> clazz1 = getClass("org.openl.generated.beans.BeanType2");
        Object defaultInstance = getInstance(clazz1);
        testValue(clazz, instance, methodName, defaultInstance);
    }
    
    private Object getInstance(Class<?> clazz) throws InstantiationException, IllegalAccessException {
        assertNotNull(clazz);
        
        Object instance = clazz.newInstance();
        return instance;
    }

}
