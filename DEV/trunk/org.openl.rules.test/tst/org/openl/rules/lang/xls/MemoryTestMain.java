/*
 * Created on Dec 30, 2003
 *
 *  Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls;

import org.openl.OpenL;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.OpenLConfiguration;
import org.openl.main.OpenlMain;
import org.openl.syntax.impl.FileSourceCodeModule;

/**
 * @author snshor
 *
 */
public class MemoryTestMain
{

	public static void main(String[] args) throws Exception
	{
		for (int i = 0; i < 20; i++)
		{
			System.out.println("############### " + i + "   ##########");

			OpenL.reset();
			OpenLConfiguration.reset();
			ClassLoaderFactory.reset();
			System.out.println("############### " + "GC" + "   ##########");
			System.gc();
			//			OpenlTest.aTestEvaluate("5.5 + 4", new Double(9.5), "org.openl.j");
			new OpenlMain("org.openl.rules.lang.xls").safeRunOpenl(
				"org.openl.rules.lang.xls",
				new FileSourceCodeModule(
					"tst/org/openl/xls/Test2.xls",
					"Test2.xls"),
				"hello",
				new Object[] { new Integer(10)});
		}
		
		System.out.println("Sleeping ...");
		
		Thread.sleep(10000000);
		System.out.println("Finished ...");
		
	}

}
