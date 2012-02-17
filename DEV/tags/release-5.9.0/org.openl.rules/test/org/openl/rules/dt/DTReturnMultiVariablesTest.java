package org.openl.rules.dt;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class DTReturnMultiVariablesTest extends BaseOpenlBuilderHelper {
	
	private static String src = "test/rules/dt/DTReturnMultiVariablesTest.xls";
	
	public DTReturnMultiVariablesTest() {
		super(src);
	}
	
	@Test
	public void testRightNumberOfParameters() {
		String result = (String)invokeMethod("validateAttribute", new IOpenClass[]{JavaOpenClass.STRING, JavaOpenClass.STRING}, new Object[]{"Driver", "denis"});
		assertEquals("15secondDomainsecondText", result);
	}
}
