/*
 * Created on April 1, 2004
 *
 * Developed by OpenRules Inc. 2004
 */

package org.openl.rules.examples.hello;

import org.openl.rules.runtime.RulesEngineFactory;

public class RunHelloExcelCustomer
{
    public interface IExample {
        void helloCustomer(Response response);
    }
    
	public static void main(String[] args)
	{
		String fileName = "rules/HelloExcelCustomer.xls";
		
        RulesEngineFactory<IExample> engineFactory = new RulesEngineFactory<IExample>(fileName, IExample.class);
        IExample instance = engineFactory.newEngineInstance();

		System.out.println(
		"\n============================================\n" +
		   fileName + "(helloCustomer)" + 
		"\n============================================\n");
		Response response = new Response();
		instance.helloCustomer(response);
		System.out.println("Response: " + 
							response.getMap().get("greeting") + ", " +
							response.getMap().get("salutation") + 
							response.getMap().get("name") + "!"		 					
							);
	}

}
