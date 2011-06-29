/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.examples.hello;

import org.openl.main.Engine;
/**
 * A simple Java test for HelloWorldWithResponse.xls
 * 
 */
public class RunHelloWorldWithResponse
{
	public static void main(String[] args)
	{
		String fileName = args[0];
		String methodName = args[1];
		Engine engine =	new Engine("org.openl.xls",fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		Response response = new Response();
		engine.run(new Object[] { response });
		System.out.println("Response: " + response.getResult());
	}
}
