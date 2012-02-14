package org.openl.rules;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class HeadersWithSpacesTest extends BaseOpenlBuilderHelper {
	private static String __src = "test/rules/test xls/Test_Headers_With_Spaces.xls";

	public HeadersWithSpacesTest() {
		super(__src);
	}
	
	@Test
	public void testDT() {
		// find the table by the header with first 3 spaces
		//
		TableSyntaxNode tsn = findTable("   Rules DoubleValue getILFactor(String coverageCD, String vehicleGroup)");
		if (tsn == null) {
			fail("Cannot find Decision table");
		} else {
			// https://jira.exigenservices.com/browse/EPBDS-3104, after fix should be DT
			assertEquals(XlsNodeTypes.XLS_OTHER, XlsNodeTypes.getEnumConstant(tsn.getType()));
			
			// test that it invokes successfully, means that it was compiled succesfully
			//
			// temporary commented till https://jira.exigenservices.com/browse/EPBDS-3104 will be fixed
			// should pass after fix
//			assertEquals(4, invokeMethod((IOpenMethod)tsn.getMember(), new Object[]{"Comp", "TR"}));
		}
	}
}
