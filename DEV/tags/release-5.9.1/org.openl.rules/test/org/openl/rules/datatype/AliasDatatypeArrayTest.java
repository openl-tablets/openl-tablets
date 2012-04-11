package org.openl.rules.datatype;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;

public class AliasDatatypeArrayTest extends BaseOpenlBuilderHelper {
	
	private static String src = "test/rules/datatype/AliasDatatypeArrayTest.xls";
	
	public AliasDatatypeArrayTest() {
		super(src);
	}
	
	@Test
	public void test() {
		String result = (String)invokeMethod("testAliasArray");
		assertEquals("Value2", result);
	}

}
