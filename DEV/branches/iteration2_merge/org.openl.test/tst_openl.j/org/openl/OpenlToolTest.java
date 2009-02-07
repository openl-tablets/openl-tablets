package org.openl;
import org.openl.OpenL;
import org.openl.OpenlTool;
import org.openl.binding.IBindingContext;
import org.openl.meta.StringValue;
import org.openl.syntax.impl.StringSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

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
	
	public void testMakeMethod()
	{
		StringValue srcCode = new StringValue("5");
		
		OpenL openl = OpenL.getInstance("org.openl.j");
		String name = "abc";
		IMethodSignature signature = IMethodSignature.VOID;
		IOpenClass declaringClass = null;
		int depthParameterSearchLevel = 1;
		IBindingContext cxt = openl.getBinder().makeBindingContext();
		
		IOpenMethod m = OpenlTool.makeMethodWithUnknownType(srcCode.asSourceCodeModule(), 
				openl, name, signature, declaringClass, depthParameterSearchLevel, cxt);
		assertEquals( JavaOpenClass.INT, m.getType());
		
		
		srcCode = new StringValue("if (true) return 5.0; else return 9.1;");
		m = OpenlTool.makeMethodWithUnknownType(srcCode.asSourceCodeModule(), 
				openl, name, signature, declaringClass, depthParameterSearchLevel, cxt);
		assertEquals( JavaOpenClass.DOUBLE, m.getType());
		
	}


}
