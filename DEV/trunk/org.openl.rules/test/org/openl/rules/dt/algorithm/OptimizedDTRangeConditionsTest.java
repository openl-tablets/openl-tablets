package org.openl.rules.dt.algorithm;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class OptimizedDTRangeConditionsTest extends BaseOpenlBuilderHelper {
	
	public static final String src = "rules/dt/algorithm/OptimizedDTRangeConditions.xls";
	
	public OptimizedDTRangeConditionsTest() {
		super(src);
	}
	
	@Test
	public void testIntRangeClosed() {		
		IOpenMethod method = getMethod("intRangeClosed", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Integer.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(1)});
		assertEquals("rule1", res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Integer.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Integer.valueOf(12)});
		assertEquals("rule1", res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Integer.valueOf(14)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Integer.valueOf(16)});
		assertEquals("rule2", res5);
		
		String res6 = (String)invokeMethod(method, new Object[]{Integer.valueOf(18)});
		assertEquals("rule2", res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Integer.valueOf(19)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Integer.valueOf(21)});
		assertEquals("rule3", res8);
		
		String res9 = (String)invokeMethod(method, new Object[]{Integer.valueOf(26)});
		assertEquals("rule3", res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Integer.valueOf(27)});
		assertNull(res10);
	}
	
	@Test
	public void testIntRangeLeftOpened() {		
		IOpenMethod method = getMethod("intRangeLeftOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Integer.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(1)});
		assertNull(res1);
		
		String res1_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(2)});
		assertEquals("rule1", res1_1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Integer.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Integer.valueOf(12)});
		assertEquals("rule1", res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Integer.valueOf(14)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Integer.valueOf(16)});
		assertNull(res5);
		
		String res5_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(17)});
		assertEquals("rule2", res5_1);
		
		String res6 = (String)invokeMethod(method, new Object[]{Integer.valueOf(18)});
		assertEquals("rule2", res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Integer.valueOf(19)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Integer.valueOf(21)});
		assertNull(res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(22)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Integer.valueOf(26)});
		assertEquals("rule3", res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Integer.valueOf(27)});
		assertNull(res10);
	}
	
	@Test
	public void testIntRangeRightOpened() {		
		IOpenMethod method = getMethod("intRangeRightOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Integer.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(1)});
		assertEquals("rule1", res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Integer.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Integer.valueOf(12)});
		assertNull(res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Integer.valueOf(14)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Integer.valueOf(16)});
		assertEquals("rule2", res5);
		
		String res5_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(17)});
		assertEquals("rule2", res5_1);
		
		String res6 = (String)invokeMethod(method, new Object[]{Integer.valueOf(18)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Integer.valueOf(19)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Integer.valueOf(21)});
		assertEquals("rule3", res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(25)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Integer.valueOf(26)});
		assertNull(res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Integer.valueOf(27)});
		assertNull(res10);
	}
	
	@Test
	public void testIntRangeOpened() {		
		IOpenMethod method = getMethod("intRangeOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Integer.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(1)});
		assertNull(res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Integer.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Integer.valueOf(12)});
		assertNull(res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Integer.valueOf(14)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Integer.valueOf(16)});
		assertNull(res5);
		
		String res5_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(17)});
		assertEquals("rule2", res5_1);
		
		String res6 = (String)invokeMethod(method, new Object[]{Integer.valueOf(18)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Integer.valueOf(19)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Integer.valueOf(21)});
		assertNull(res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Integer.valueOf(25)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Integer.valueOf(26)});
		assertNull(res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Integer.valueOf(27)});
		assertNull(res10);
	}
	
	@Test
	public void testDoubleRangeClosed() {		
		IOpenMethod method = getMethod("doubleRangeClosed", new IOpenClass[]{JavaOpenClass.getOpenClass(Double.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Double.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Double.valueOf(1)});
		assertEquals("rule1", res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Double.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Double.valueOf(15)});
		assertEquals("rule1", res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.1)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5)});
		assertEquals("rule2", res5);
		
		String res6 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5555)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.5555)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.6)});
		assertEquals("rule3", res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.1)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5)});
		assertEquals("rule3", res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5001)});
		assertNull(res10);
	}
	
	
	@Test
	public void testDoubleRangeLeftOpened() {		
		IOpenMethod method = getMethod("doubleRangeLeftOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Double.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Double.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Double.valueOf(1)});
		assertNull(res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Double.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Double.valueOf(15)});
		assertEquals("rule1", res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.1)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5)});
		assertEquals("rule2", res5);
		
		String res6 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5555)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.5555)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.6)});
		assertNull(res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.1)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5)});
		assertEquals("rule3", res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5001)});
		assertNull(res10);
	}
	
	@Test
	public void testDoubleRangeRightOpened() {		
		IOpenMethod method = getMethod("doubleRangeRightOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Double.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Double.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Double.valueOf(1)});
		assertEquals("rule1", res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Double.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Double.valueOf(15)});
		assertNull(res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.1)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5)});
		assertEquals("rule2", res5);
		
		String res6 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5555)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.5555)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.6)});
		assertEquals("rule3", res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.1)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5)});
		assertNull(res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5001)});
		assertNull(res10);
	}
	
	@Test
	public void testDoubleRangeOpened() {		
		IOpenMethod method = getMethod("doubleRangeOpened", new IOpenClass[]{JavaOpenClass.getOpenClass(Double.class)});
		String res0 = (String)invokeMethod(method, new Object[]{Double.valueOf(0)});
		assertNull(res0);
		
		String res1 = (String)invokeMethod(method, new Object[]{Double.valueOf(1)});
		assertNull(res1);
		
		String res2 = (String)invokeMethod(method, new Object[]{Double.valueOf(10)});
		assertEquals("rule1", res2);
		
		String res3 = (String)invokeMethod(method, new Object[]{Double.valueOf(15)});
		assertNull(res3);
		
		String res4 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.1)});
		assertNull(res4);
		
		String res5 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5)});
		assertEquals("rule2", res5);
		
		String res6 = (String)invokeMethod(method, new Object[]{Double.valueOf(15.5555)});
		assertNull(res6);
		
		String res7 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.5555)});
		assertNull(res7);
		
		String res8 = (String)invokeMethod(method, new Object[]{Double.valueOf(16.6)});
		assertNull(res8);
		
		String res8_1 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.1)});
		assertEquals("rule3", res8_1);
		
		String res9 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5)});
		assertNull(res9);
		
		String res10 = (String)invokeMethod(method, new Object[]{Double.valueOf(17.5001)});
		assertNull(res10);
	}
	
	
}
