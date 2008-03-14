/*
 * Created on April 1, 2004
 *
 * Developed by OpenRules Inc. 2004
 */

package org.openl.rules.examples.hello;

import org.openl.main.Engine;
/**
 * A simple Java test for HelloExcelCustomer.xls
 * 
 * @author jf
 */

public class RunHelloExcelCustomer
{
	public static void main(String[] args)
	{
		String fileName = "rules/HelloExcelCustomer.xls";
		String methodName = "helloCustomer";
		Engine engine =	new Engine("org.openl.xls", fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		Response response = new Response();
		engine.run(new Object[] { response });
		System.out.println("Response: " + 
							response.getMap().get("greeting") + ", " +
							response.getMap().get("salutation") + 
							response.getMap().get("name") + "!"		 					
							);
	}

}
