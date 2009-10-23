/*
 * Created on Aug 1, 2003
 *
 * Developed by OpenRules Inc. 2003-2004
 */
 
package org.openl.examples;

import org.openl.OpenL;
import org.openl.syntax.impl.StringSourceCodeModule;


/**
 * @author snshor
 *
 * This is  a simplest example of how to use OpenL from Java application.
 * 
 * It shows how to use an OpenL language configuration org.openl.j 
 * and evaluate a traditional "Hello, World!" application. 	
 *
 */
public class HelloWorld
{


	public static void main(String[] args) throws Exception
	{

			// This is code we are going to evaluate
			String openLcode = "System.out.println(\"Hello, World!\")";

			// Get an OpenL language configuration named "org.openl.j"
			OpenL language = OpenL.getInstance("org.openl.j");

			//evaluate code, see result on console
			language.evaluate(new StringSourceCodeModule(openLcode, null));

	}


}
