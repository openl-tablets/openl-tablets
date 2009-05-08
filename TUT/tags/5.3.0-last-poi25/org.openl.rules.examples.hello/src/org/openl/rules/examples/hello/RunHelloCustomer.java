/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.examples.hello;


import org.openl.main.Engine;

/**
 * A simple Java test for HelloCustomer.xls
 * 
 * @author jf
 */

public class RunHelloCustomer
{
	public static void main(String[] args)
	{
		String fileName = "rules/HelloCustomer.xls";
		String methodName = "helloCustomer";
		Engine engine =	new Engine("org.openl.xls",fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		Customer customer = new Customer();
		customer.setName("Robinson");
		customer.setGender("Female");
		customer.setMaritalStatus("Married");
		
		Response response = new Response();
		engine.run(new Object[] { customer, response });
		System.out.println("Response: " + 
		 					response.getMap().get("greeting") + ", " +
							response.getMap().get("salutation") + 
							customer.getName() + "!"		 					
		 					);
	}
}
