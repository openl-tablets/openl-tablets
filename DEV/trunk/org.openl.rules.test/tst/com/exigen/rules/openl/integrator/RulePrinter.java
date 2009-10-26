/**
 * Created Apr 3, 2007
 */
package com.exigen.rules.openl.integrator;

import org.openl.OpenL;
import org.openl.rules.dt.DTRule;
import org.openl.rules.dt.DTRuleQuery;
import org.openl.rules.dt.DecisionTable;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;

/**
 * @author snshor
 *
 */
public class RulePrinter
{

	public static void main(String[] args)
	{
		IOpenClass ioc = OpenL.getInstance("org.openl.xls").compileModule(
				new FileSourceCodeModule("TestRule.xls",
						null));
		new RulePrinter().printRule(ioc, "authority", 1);
		
		DTRule[] rules = DTRuleQuery.selectRulesWithParam(ioc, null, "Client Age" , null);
		
		for (int i = 0; i < rules.length; i++)
		{
			printDTRule(rules[i]);
			System.out.println("<p/>");
		}
		
	}

	/**
	 * @param ioc
	 * @param string
	 * @param i
	 */
	private void printRule(IOpenClass ioc, String name, int i)
	{
		IOpenMethod om = AOpenClass.getSingleMethod(name, ioc.methods());
		DecisionTable dt = (DecisionTable)om;
		
		printDTRule(dt, i);
		
	}

	static void printDTRule(DTRule rule)
	{
		System.out.println(rule.display(new StringBuffer(), "HTML"));
	}	
	
	/**
	 * @param dt
	 * @param i
	 */
	private void printDTRule(DecisionTable dt, int ruleRow)
	{
		
		DTRule rule = new DTRule(dt, ruleRow);
		
		
		System.out.println(rule.display(new StringBuffer(), "HTML"));
		
//		System.out.print("if ");
//		
//		for (int j = 0; j < dt.getConditionRows().length; j++)
//		{
//			IDTCondition dtc = dt.getConditionRows()[j];
//			FunctionalRow ca = (FunctionalRow)dtc;
////			CompositeMethod cm = (CompositeMethod) ca.getMethod();
//
//			
//			IParameterDeclaration[] params = ca.getParams();
//			
//			
//			if (j > 0)
//				System.out.print(" AND ");
//			for (int i = 0; i < params.length; i++)
//			{
//				DTParameterInfo pi = ca.getParameterInfo(i);
//				
//				System.out.print(pi.getPresentation());
//				System.out.print(" ");
//				System.out.print(pi.getValue(rule));
//			}
//			
//		}
//		System.out.println("");
	}
	
	
	
	
	
	
	
}
