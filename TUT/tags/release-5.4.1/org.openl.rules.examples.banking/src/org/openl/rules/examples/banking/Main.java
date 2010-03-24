/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules, Inc. 2003
 */

package org.openl.rules.examples.banking;

import org.openl.main.Engine;



/**
 * @author snshor
 *
 */
public class Main
{
	public static void main(String[] args)
	{
		System.out.println("*** Resolve Banking Problem ***");
		String fileName = "rules/Banking.xls";
		String methodName = "resolveProblem";
		Engine engine1 = new Engine("org.openl.xls" ,fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		Response response1 = new Response();
		engine1.run(new Object[] { response1 });
		System.out.println("Response:");
		System.out.println(response1);
		
		System.out.println("*** UpSell Banking Products ***");
		methodName = "upSell";
		Engine engine2 = new Engine("org.openl.xls",fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		Response response2 = new Response();
		engine2.run(new Object[] { response2 });
		System.out.println("Response:");
		System.out.println(response2);		

	}
}


