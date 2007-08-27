/**
 * Created Jan 3, 2007
 */
package org.openl.rules.testmethod;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.binding.DataTableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

/**
 * @author snshor
 *
 */
public class TestMethodBoundNode extends DataTableBoundNode
{
	
	TestMethodHelper tmhelper;

	/**
	 * @param dtNode
	 * @param table
	 * @param module
	 */
	public TestMethodBoundNode(TableSyntaxNode dtNode, 
			XlsModuleOpenClass module)
	{
		super(dtNode, module);
	}

	public void addTo(ModuleOpenClass openClass)
	{
		super.addTo(openClass);
		tmhelper.setBoundNode(this);
		openClass.addMethod(tmhelper.getTestAll());
	}

	public TestMethodHelper getTmhelper()
	{
		return this.tmhelper;
	}

	public void setTmhelper(TestMethodHelper tmhelper)
	{
		this.tmhelper = tmhelper;
	}

	
	
	
	
}
