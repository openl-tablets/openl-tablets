/*
 * Created on Oct 2, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.lang.xls;

import java.io.File;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.openl.IOpenBinder;
import org.openl.IOpenParser;
import org.openl.IOpenSourceCodeModule;
import org.openl.OpenL;
import org.openl.binding.IBoundCode;
import org.openl.syntax.IParsedCode;
import org.openl.syntax.ISyntaxError;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.test.OpenlTest;
import org.openl.xls.OpenLBuilder;

/**
 * @author snshor
 *
 */
public class TestParser extends TestCase
{

  /**
   * Constructor for TestParser.
   * @param name
   */
  public TestParser(String name)
  {
    super(name);
  }


	public int _testOpenlParse(String fileName) throws Exception 
	{
//		OpenL openl = OpenL.getInstance("org.openl.rules.lang.xls");
		
	  
	  	System.out.println(OpenLBuilder.class.getName());
	//    Class.forName("org.openl.rules.xls.lang.XlsOpenLBuilder");
		OpenL openl = OpenL.getInstance("org.openl.xls");
		IOpenParser parser = openl.getParser();
		
		
		
		FileSourceCodeModule scm = new FileSourceCodeModule(new File(fileName), null);		
		
		IParsedCode pc = parser.parseAsModule(scm);
		
		ISyntaxError[] err = pc.getError();
		for (int i = 0; i < err.length; i++)
    {
      printSyntaxError(err[i]);
    }
    
    return err.length;
	}


	public int _testOpenlBind(String fileName) throws Exception 
	{
		OpenL openl = OpenL.getInstance("org.openl.xls");
		
		IOpenParser parser = openl.getParser();
		
		
		
		IOpenSourceCodeModule scm = new FileSourceCodeModule(new File(fileName), null);		
		
		IParsedCode pc = parser.parseAsModule(scm);
		
		ISyntaxError[] err = pc.getError();
		for (int i = 0; i < err.length; i++)
		{
			printSyntaxError(err[i]);
		}
		Assert.assertEquals(0, err.length);

		IOpenBinder binder = openl.getBinder();
		
		IBoundCode bc = binder.bind(pc);
		
		err = bc.getError();
		for (int i = 0; i < err.length; i++)
		{
			printSyntaxError(err[i]);
		}
    
		return err.length;
	}
	
	
	void printSyntaxError(ISyntaxError err)
	{
		System.out.println(err.getMessage());
		((Throwable)err).printStackTrace();
		
//		OpenLRuntimeException.printSourceLocation(err.getLocation(), err.getModule(), new PrintWriter( System.out, true));
		if (err.getThrowable() != null)
		{
			Throwable t = ExceptionUtils.getCause(err.getThrowable());
			t = t == null ? err.getThrowable() : t;
			t.printStackTrace();
		}
	}
	

	
		public void testOpenlParse() throws Exception
		{
			Assert.assertEquals(0, 	_testOpenlParse("tst/org/openl/rules/lang/xls/Test1.xls"));			
			Assert.assertEquals(0, 	_testOpenlParse("tst/org/openl/rules/lang/xls/Test2.xls"));			
		}
		 
	
	public void testOpenlBind() throws Exception
	{
		Assert.assertEquals(0, 	_testOpenlBind("tst/org/openl/rules/lang/xls/Test2.xls"));			
	}
		 
	
	public void testOpenlRun() throws Exception
	{
		OpenlTest.aTestMethodFile("tst/org/openl/rules/lang/xls/Test2.xls",  "org.openl.xls", "hello", new Object[]{new Integer(14)}, "Y5");
		
	}
		 
   public static void main(String[] args) throws Exception 
   {
	   TestParser tp = new TestParser("zz");
	   tp.testOpenlParse();
	   tp.testOpenlBind();
   }   
	
}

	


