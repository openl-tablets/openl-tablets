package org.openl;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.types.IOpenClass;

import junit.framework.Assert;
import junit.framework.TestCase;

/*
 * Created on Mar 11, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

/**
 * @author snshor
 *
 */
public class OpenlToolTest extends TestCase
{

	/**
	 * Constructor for OpenlToolTest.
	 * @param name
	 */
	public OpenlToolTest(String name)
	{
		super(name);
	}
	
	public void testMakeType()
	{
		String type = "String [] []";
		
		OpenL openl = OpenL.getInstance("org.openl.j");
		
		
		String[][] xx = {{""}};
		
		IOpenClass ioc = OpenlTool.makeType(new StringSourceCodeModule(type, "<internal_string>"), openl, null);
		Assert.assertEquals(xx.getClass(), ioc.getInstanceClass());
		
		type = "String [] [] xyz";
		ioc = OpenlTool.makeType(new StringSourceCodeModule(type, "<internal_string>"), openl, null);
		Assert.assertEquals(xx.getClass(), ioc.getInstanceClass());
		
	}
	



}
