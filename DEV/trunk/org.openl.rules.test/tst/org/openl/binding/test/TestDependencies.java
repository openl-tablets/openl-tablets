/**
 * Created Dec 3, 2006
 */
package org.openl.binding.test;

import java.util.Iterator;

import junit.framework.TestCase;

import org.openl.base.INamedThing;
import org.openl.binding.BindingDependencies;
import org.openl.main.SourceCodeURLConstants;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.ITracerObject;
import org.openl.vm.Tracer;

import all.tests.Tools;

/**
 * @author snshor
 *
 */
public class TestDependencies extends TestCase
{

	public void testDeps()
	{
		XlsModuleOpenClass xmo = _createModule();
		
		for (Iterator iter = xmo.methods(); iter.hasNext();)
		{
			IOpenMethod m = (IOpenMethod) iter.next();
			BindingDependencies bd = new BindingDependencies();
			
			if (m instanceof DecisionTable	)	{
				DecisionTable dt = (DecisionTable) m;
				dt.updateDependency(bd);
			}
			else if (m instanceof CompositeMethod)	{
				CompositeMethod cm = (CompositeMethod) m;
				cm.updateDependency(bd);
			}	
			else
			{
				System.out.println("Method " + m.getName() + " has type " + m.getClass());
				continue;
			}	
			System.out.println();
			System.out.println(m.getName());
			System.out.println(bd);
		}
	}

	static String fname = "tst/org/openl/binding/test/TestBinding.xls";

	/**
	 * @return
	 */
	private XlsModuleOpenClass _createModule()
	{		
		return Tools.createModule(fname); 
	}
	
	
	void _testTracer() throws Exception
	{
		Tracer t = new Tracer();
		Tracer.setTracer(t);
		
		Object res =Tools.run(fname, "hello1", new Object[]{new Integer(23)});
		System.out.println(res);
		
		ITracerObject[] tt = t.getTracerObjects();
		
		for (int i = 0; i < tt.length; i++)
		{
			printTO(tt[i], 0);
		}
		
	}
	
	void printTO(ITracerObject to, int level)
	{
		for (int i = 0; i < level * 2; i++)
		{
			System.out.print(' ');
		}
		
		System.out.println("TRACE: " + to.getDisplayName(INamedThing.REGULAR));
		System.out.println(SourceCodeURLConstants.AT_PREFIX + to.getUri());
		
		
		ITracerObject[] tt = to.getTracerObjects();
		
		for (int i = 0; i < tt.length; i++)
		{
			printTO(tt[i], level+1);
		}
		
	}
	
	
	
	public static void main(String[] args) throws Exception
	{
		new TestDependencies()._testTracer();
	}
	
}
