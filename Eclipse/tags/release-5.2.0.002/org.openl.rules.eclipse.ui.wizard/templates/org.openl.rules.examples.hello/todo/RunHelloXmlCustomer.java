/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.examples.hello;

import org.openl.main.Engine;


/**
 * A simple Java test for HelloXmlCustomer.xls
 * 
 * @author jf
 */

public class RunHelloXmlCustomer
{
	public static void main(String[] args)
	{
		String fileName = "rules/HelloXmlCustomer.xls";
		String methodName = "run";
		Engine engine =	new Engine("org.openl.xls",fileName,methodName);
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
