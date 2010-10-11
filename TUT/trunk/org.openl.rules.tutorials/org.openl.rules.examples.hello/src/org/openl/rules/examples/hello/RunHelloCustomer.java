/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.examples.hello;


import org.openl.main.Engine;
import org.openl.rules.examples.hello.RunHelloExcelCustomer.IExample;
import org.openl.rules.runtime.RuleEngineFactory;
import org.openl.runtime.EngineFactory;

/**
 * A simple Java test for HelloCustomer.xls
 * 
 * @author jf
 */

public class RunHelloCustomer
{
    public interface IExample {
        void helloCustomer(Customer customer, Response response);
    }
    
	public static void main(String[] args)
	{
		String fileName = "rules/HelloCustomer.xls";

	    EngineFactory<IExample> engineFactory = new RuleEngineFactory<IExample>(fileName, IExample.class);
        IExample instance = engineFactory.makeInstance();

        System.out.println(
		"\n============================================\n" +
		   fileName + "(helloCustomer)" + 
		"\n============================================\n");
		Customer customer = new Customer();
		customer.setName("Robinson");
		customer.setGender("Female");
		customer.setMaritalStatus("Married");
		
		Response response = new Response();
		instance.helloCustomer(customer, response);
		System.out.println("Response: " + 
		 					response.getMap().get("greeting") + ", " +
							response.getMap().get("salutation") + 
							customer.getName() + "!"		 					
		 					);
	}
}
