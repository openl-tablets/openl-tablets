package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.message.OpenLMessage;
import org.openl.rules.BaseOpenlBuilderHelper;

public class WrongAliasDatatypeArrayTest extends BaseOpenlBuilderHelper {
	
	private static String src = "test/rules/datatype/WrongAliasDatatypeArrayTest.xls";
	
	public WrongAliasDatatypeArrayTest() {
		super(src);
	}
	
	@Test
	public void test() {
		assertTrue(getJavaWrapper().getCompiledClass().getBindingErrors().length == 1);
		assertTrue(getJavaWrapper().getCompiledClass().getMessages().size() == 1);
		OpenLMessage message = getJavaWrapper().getCompiledClass().getMessages().get(0);
		assertNotNull(message);
		assertEquals("Object Val23 is outside of a valid domain", message.getSummary());
	}
	
}
