package org.openl.rules.dt;

import java.util.Iterator;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.table.ILogicalTable;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.java.JavaOpenClass;

public class DTCondition extends FunctionalRow implements IDTCondition
{
	
	
	

	public DTCondition(String name, int row, ILogicalTable decisionTable)
	{
		super(name, row, decisionTable);
	}

	public boolean isAction()
	{
		return false;
	}

	public boolean isCondition()
	{
		return true;
	}

/**
 * 	Since version 4.0.2 the Condition can have an optimized form. 
 * 	In this case the Condition Expression(CE) can have type that is different 
 *  from boolean. For details consult DTOptimizedAlgorithm class
 *  
 *  The algorithm for Condition Expression parsing will work like this:
 *  <p>
 *  1) Compile CE as <code>void</code> expression with all the Condition Parameters(CP). Report errors anad return, if any
 *  <p>
 *  2) Check if the expression depends on any of the parameters, if it does,
 *  it is not intended to be optimized (at least not in this version). In this case 
 *  it has to have <code>boolean</code> type.
 *  <p>
 *  3) Try to find possible expression/optimization for the combination of CE and CP types. 
 *  See DTOptimizedAlgorithm for the full set of available optimizations. If not found - raise exception. 
 *  <p>
 *  4) Attach, expression/optimization to the Condition; change the expression header to remove CP, because they are not needed.  
 *  
 * @see DTOptimizedAlgorithm 
 */	
	
	public IDTConditionEvaluator prepareCondition(IMethodSignature signature, OpenL openl, ModuleOpenClass dtModule, IBindingContextDelegator cxtd, RuleRow ruleRow) throws Exception
	{
		
		super.prepare(NullOpenClass.the, signature, openl, dtModule, cxtd, ruleRow);
		IOpenClass methodType = method.getBodyType();
		if (isDependentOnAnyParams())
		{
			if (methodType != JavaOpenClass.BOOLEAN)
				throw new Exception("Condition must have boolean type if it depends on it's parameters");
			
			return new DefaultConditionEvaluator();
		}	
		
		evaluator = makeOptimizedConditionMethodEvaluator(signature);
		IDTConditionEvaluator dtcev = DTOptimizedAlgorithm.makeEvaluator(this, methodType);
		
		return dtcev;
		
	}
	
	boolean isDependentOnAnyParams()
	{
		IParameterDeclaration[] params = getParams();
		
		BindingDependencies deps = new BindingDependencies();

		method.updateDependency(deps);

		for (Iterator iter = deps.getFieldsMap().values().iterator(); iter.hasNext();)
		{
			IOpenField f = (IOpenField) iter.next();
			
			if (f instanceof ILocalVar)
			{
				for (int i = 0; i < params.length; i++)
				{
					if (params[i].getName().equals(f.getName()))
						return true;
				}
			}	
		}
		
		
		return false;
	}
	
	
	IMethodCaller evaluator = null;
	
  IMethodCaller makeOptimizedConditionMethodEvaluator(IMethodSignature signature)
  {
  	
  	String code = method.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
  	
  	for (int i = 0; i < signature.getNumberOfArguments(); i++)
		{
			String pname = signature.getParameterName(i);
			if (pname.equals(code))
				return new ParameterMethodCaller(method, i);
		}  
  	
		return null;
  }

	public IMethodCaller getEvaluator()
	{
		return evaluator == null ? method : evaluator;
	}
	
	

}
