package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.meta.DoubleValue;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

public class WrongAliasDatatypeArrayTest extends BaseOpenlBuilderHelper {
	
	private static String src = "test/rules/datatype/WrongAliasDatatypeArrayTest.xls";
	
	public WrongAliasDatatypeArrayTest() {
		super(src);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void test() {
		assertTrue(getCompiledOpenClass().getBindingErrors().length == 1);
		assertTrue(getCompiledOpenClass().getMessages().size() == 1);
		OpenLMessage message = getCompiledOpenClass().getMessages().iterator().next();
		assertNotNull(message);
		assertEquals("The value 'Val23' is outside of valid domain 'TestAlias1'. Valid values: [Val1, Val2, Val3]", message.getSummary());
	}
	
	@Test
	public void test1() {
		IOpenMethod method = getMethod("PhysicalDamageComprehensiveFactor", new IOpenClass[]{JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.getOpenClass(Integer.class), JavaOpenClass.STRING});
		assertNotNull(method);
		DoubleValue res = (DoubleValue)invokeMethod(method, new Object[]{500, 80000, "COMPREHENSIVE ONLY"});
		
		assertNotNull(res);
		assertEquals(1.15, res.doubleValue(), 0.01);
	}
	
	@Test
	public void test2() {		
		String[][] res = (String[][])invokeMethod("test2Dim");
		assertNotNull(res);
	}
	
	@Test
	public void test3() {
		String[] res = (String[])invokeMethod("testCast");
		assertNotNull(res);
	}
}
