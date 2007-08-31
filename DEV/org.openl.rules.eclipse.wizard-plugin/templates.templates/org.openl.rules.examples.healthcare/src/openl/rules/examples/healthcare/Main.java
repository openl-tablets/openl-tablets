/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package openl.rules.examples.healthcare;

import org.openl.main.Engine;

public class Main
{
	public static void main(String[] args)
	{
		String fileName = "rules/HealthCare.xls";
		String methodName = "main";
		Engine engine =	new Engine("org.openl.xls", fileName,methodName);
		System.out.println(
		"\n============================================\n" +
		   fileName + "(" + methodName + ")" + 
		"\n============================================\n");
		engine.run(new String[]{""});
	}
}
