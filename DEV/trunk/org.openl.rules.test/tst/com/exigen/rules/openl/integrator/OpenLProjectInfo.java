package com.exigen.rules.openl.integrator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.openl.OpenL;
import org.openl.main.OpenLWrapper;
import org.openl.rules.dt.DTAction;
import org.openl.rules.dt.DTCondition;
import org.openl.rules.dt.DTParameterInfo;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.FunctionalRow;
import org.openl.rules.dt.IDTAction;
import org.openl.rules.dt.IDTCondition;
import org.openl.rules.dt.IDecisionRow;
import org.openl.syntax.impl.FileSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class OpenLProjectInfo
{

	public static OpenLProjectInfo loadWrapper(String wName)
			throws ClassNotFoundException, InstantiationException,
			IllegalAccessException
	{
		OpenLProjectInfo pi = new OpenLProjectInfo();
		pi.load(wName);
		return pi;
	}

	void load(String openlWrapperName) throws ClassNotFoundException,
			InstantiationException, IllegalAccessException
	{
		Class wrapperClass = Class.forName(openlWrapperName);

		Object ww = wrapperClass.newInstance();

		OpenLWrapper wrapper = (OpenLWrapper) ww;

		load(wrapper.getOpenClass());

	}

	void load(IOpenClass ioc)
	{
		loadDecisionTables(ioc);
	}

	private void loadDecisionTables(IOpenClass ioc)
	{
		for (Iterator iter = ioc.methods(); iter.hasNext();)
		{
			IOpenMethod m = (IOpenMethod) iter.next();
			if (m instanceof DecisionTable)
			{
				System.out.println(m.getName());
				loadDT((DecisionTable) m);
			}

		}
	}

	private void loadDT(DecisionTable table)
	{
		IOpenClass type = table.getType();
		if (type != JavaOpenClass.VOID)
		{
			System.out
					.println("WARNING: Only Decision Tables with type 'void' can be converted");
			return;
		}
		IMethodSignature signature = table.getSignature();
		IOpenClass[] pTypes = signature.getParameterTypes();
		for (int i = 0; i < pTypes.length; i++)
		{
			if (pTypes[i] instanceof JavaOpenClass)
			{
				System.out.print("  " + pTypes[i].getName());
				System.out.println(" " + signature.getParameterName(i));
				types.add(pTypes[i]);
			} else
			{
				System.out.println("WARNING: Type " + pTypes[i].getName()
						+ " is not a Java Class");
				// return;

			}
		}

		if (!loadConditions(table))
			return;
		if (!loadActions(table))
			return;
		decisionTables.put(table.getHeader().getName(), table);

	}

	boolean loadConditions(DecisionTable dt)
	{
		IDTCondition[] cc = dt.getConditionRows();

		for (int i = 0; i < cc.length; i++)
		{
			if (!(cc[i] instanceof DTCondition))
			{
				System.out.println("WARNING: Condition is not a DTCondition: "
						+ cc[i].getName());
				return false;
			}
			if (!loadConditionOrAction((DTCondition) cc[i]))
				return false;
		}

		return true;
	}

	boolean loadActions(DecisionTable dt)
	{
		IDTAction[] cc = dt.getActionRows();

		for (int i = 0; i < cc.length; i++)
		{
			if (!(cc[i] instanceof DTAction))
			{
				System.out.println("WARNING: Action is not a DTAction: "
						+ cc[i].getName());
				return false;
			}
			if (!loadConditionOrAction((DTAction) cc[i]))
				return false;
		}

		return true;
	}

	private boolean loadConditionOrAction(FunctionalRow ca)
	{
		System.out.print("     " + ca.getName());
		// System.out.println(" "+ca.getCode().getClass().getName());

		CompositeMethod cm = (CompositeMethod) ca.getMethod();

		IParameterDeclaration[] params = ca.getParams();

		for (int i = 0; i < params.length; i++)
		{
			if (!(params[i].getType() instanceof JavaOpenClass))
			{
				System.out.println("Type " + params[i].getType().getName());
			}
			System.out.print(i == 0 ? "(" : ", ");
			System.out.print(params[i].getType().getInstanceClass().getName());
			System.out.print(" " + params[i].getName());
			System.out.print(" : " + ca.getParamPresentation()[i]);
		}
		System.out.println(")");

		System.out.println("     "
				+ cm.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode());

		Object[][] paramValues = ca.getParamValues();

		int len = paramValues.length;

		for (int j = 0; j < len; ++j)
		{
			for (int i = 0; i < params.length; i++)
			{

				// Object[] paramRow = paramValues[i];
				DTParameterInfo pi = ca.getParameterInfo(i);

				System.out.print("   " + pi.getValue(j));
			}
			System.out.println();
		}

		return true;
	}

	public static String getCode(IOpenMethod m)
	{
		return ((CompositeMethod) m).getMethodBodyBoundNode().getSyntaxNode()
				.getModule().getCode();
	}

	HashMap decisionTables = new HashMap();

	HashSet types = new HashSet();

	public IParameterDeclaration[] getParameters(IOpenMethod m)
	{
		IMethodSignature signature = m.getSignature();
		IOpenClass[] pTypes = signature.getParameterTypes();

		ParameterDeclaration[] pd = new ParameterDeclaration[pTypes.length];

		for (int i = 0; i < pTypes.length; i++)
		{
			pd[i] = new ParameterDeclaration(pTypes[i], signature.getParameterName(i));
		}
		return pd;

	}

	public DecisionTable getDecisionTable(String name) throws Exception
	{
		DecisionTable dt = (DecisionTable) decisionTables.get(name);
		if (dt == null)
			throw new Exception("DecisionTable " + name + " not found");
		return dt;
	}

	public Iterator allTables()
	{
		return decisionTables.values().iterator();
	}

	public int getNumberOfRules(DecisionTable table)
	{
		if (table.getConditionRows().length > 0)
			return getParamValues(table.getConditionRows()[0]).length;
		if (table.getActionRows().length > 0)
			return getParamValues(table.getActionRows()[0]).length;
		return 0;
	}

	public Object[][] getParamValues(IDecisionRow row)
	{
		return ((FunctionalRow) row).getParamValues();
	}

	public static void main(String[] args)
	{
		IOpenClass ioc = OpenL.getInstance("org.openl.xls").compileModule(
				new FileSourceCodeModule("tst/org/openl/rules/lang/xls/IndexLogic.xls",
						null));
		new OpenLProjectInfo().load(ioc);
	}
}
