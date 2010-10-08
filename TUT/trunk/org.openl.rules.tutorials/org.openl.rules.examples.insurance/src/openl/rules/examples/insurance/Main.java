/*
 * Created on Dec 4, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package openl.rules.examples.insurance;

import org.openl.main.Engine;

public class Main {
	public static void main(String[] args) {
		String fileName = "rules/Insurance.xls";
		String methodName = "main";
		Class<?>[] paramTypes = new Class<?>[] {String[].class};
        Engine engine = new Engine("org.openl.xls", fileName, methodName, paramTypes);
		System.out.println(
			"\n============================================\n"
				+ fileName
				+ "("
				+ methodName
				+ ")"
				+ "\n============================================\n");
		engine.run(new String[] { "" });
	}
}
