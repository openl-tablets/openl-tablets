/*
 * Created on Dec 30, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import java.util.HashMap;

import junit.framework.TestCase;

import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.main.OpenlMain;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class MemoryTest extends TestCase
{

	/**
	 * Constructor for MemoryTest.
	 * @param name
	 */
	public MemoryTest(String name)
	{
		super(name);
	}

	public void testMemory() throws Exception
	{
		for (int i = 0; i < 100; i++)
		{
			System.out.println("############### " + i + "   ##########");

			OpenL.reset();
			OpenLConfiguration.reset();
			HashMap old = ClassLoaderFactory.reset();
			JavaOpenClass.resetAllClassloaders(old);
			
			System.out.println("############### " + "GC" + "   ##########");
			System.gc();
			//			OpenlTest.aTestEvaluate("5.5 + 4", new Double(9.5), "org.openl.j");
			new OpenlMain("org.openl.xls").safeRunOpenl(
				"org.openl.xls",
				new FileSourceCodeModule(
					"tst/org/openl/rules/lang/xls/Test2.xls",
					null),
				"hello",
				new Object[] { new Integer(10)});
		}

		JavaOpenClass.printCache();

	}

}
