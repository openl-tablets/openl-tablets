/*
 * Created on Sep 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.rules.dt;

import org.openl.OpenL;
import org.openl.base.INamedThing;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.domain.IIntIterator;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.print.Formatter;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.ITracerObject;
import org.openl.vm.Tracer;

/**
 * @author snshor
 * 
 */
public class DecisionTable implements IOpenMethod, IDecisionTable,
		IDecisionTableConstants, IMemberMetaInfo, IXlsTableNames
{
	IOpenMethodHeader header;

	static final int UNDEFINED = 0, FALSE = 1, TRUE = 2, NA = 3,
			SPECIAL_FALSE = 4, SPECIAL_TRUE = 5;

	static final int COLUMN_MODE = 0, ROW_MODE = 1;

	IDTCondition[] conditionRows;

	IDTAction[] actionRows;

	RuleRow ruleRow;

	// IDecisionTableStructure structure;
	int columns;

	private TableSyntaxNode tableSyntaxNode;

	/**
	 * @param name
	 * @param typeClass
	 * @param parameterTypes
	 * @param declaringClass
	 */
	private DecisionTable(IOpenMethodHeader header)
	{
		this.header = header;
	}

	public void bindTable(IDTCondition[] conditionRows, IDTAction[] actionRows,
			RuleRow ruleRow,
			// IDecisionTableStructure structure,
			OpenL openl, ModuleOpenClass module, IBindingContextDelegator cxtd,
			int columns) throws Exception
	{
		this.conditionRows = conditionRows;
		this.actionRows = actionRows;
		this.ruleRow = ruleRow;
		// this.structure = structure;
		this.columns = columns;
		prepare(header, openl, module, cxtd);
	}

	public int getNumberOfRules() 
	{
		if (conditionRows.length > 0)
			return conditionRows[0].getParamValues().length;
		if ( actionRows.length > 0)
			return actionRows[0].getParamValues().length;
		return 0;
	}
	
	
	// public Object getResult(int row, int col)
	// {
	// }
	int getColumns()
	{
		return columns;
	}

	// public IDecisionTableStructure getStructure()
	// {
	// return structure;
	// }
	public void prepare(IOpenMethodHeader header, OpenL openl,
			ModuleOpenClass dtModule, IBindingContextDelegator cxtd) throws Exception
	{
		IMethodSignature signature = header.getSignature();
		
		IDTConditionEvaluator[] evs = new IDTConditionEvaluator[conditionRows.length];

		for (int i = 0; i < conditionRows.length; i++)
		{
			
			
			evs[i] = conditionRows[i].prepareCondition(signature, openl,
					dtModule, cxtd, ruleRow);
		}
		
		makeAlgorithm(evs);
		
		
		for (int i = 0; i < actionRows.length; i++)
		{
			IOpenClass methodType = actionRows[i].isReturnAction() ? header.getType()
					: JavaOpenClass.VOID;

			actionRows[i].prepareAction(methodType, signature, openl, dtModule, cxtd,
					ruleRow);
		}
	}


	String printMask(boolean[] mask)
	{
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		for (int i = 0; i < mask.length; i++)
		{
			if (i % 5 == 0 && i != 0)
				buf.append(' ');
			buf.append(mask[i] ? 'T' : 'f');
		}
		buf.append(']');
		return buf.toString();
	}

//	public Object invoke2(Object target, Object[] params, IRuntimeEnv env)
//	{
//		boolean[] mask = new DecisionTableAlgorithm(conditionRows.length,
//				getColumns(), conditionRows, target, params, env).calculateTable();
//		if (Log.isDebugEnabled())
//		{
//			Log.debug(header.getName());
//			Log.debug(printMask(mask));
//		}
//		Object ret = null;
//		for (int i = 0; i < mask.length; i++)
//		{
//			if (!mask[i])
//				continue;
//			for (int j = 0; j < actionRows.length; j++)
//			{
//				ret = actionRows[j].executeAction(i, target, params, env);
//				if (ret != null && actionRows[j].isReturnAction())
//					return ret;
//			}
//		}
//		return ret;
//	}

	public Object invoke(Object target, Object[] params, IRuntimeEnv env)
	{
		if (Tracer.isTracerOn())
			return invokeTracedOptimized(target, params, env);
		return invokeOptimized(target, params, env);
	}

	public Object invokeStandard(Object target, Object[] params, IRuntimeEnv env)
	{
		int nRules = getColumns();
		DecisionTableAlgorithm dta = new DecisionTableAlgorithm(
				conditionRows.length, nRules, conditionRows, target, params, env);
		Object ret = null;

		for (int rule = 0; rule < nRules; rule++)
		{
			if (!dta.calcColumn(rule))
				continue;
			for (int j = 0; j < actionRows.length; j++)
			{
				ret = actionRows[j].executeAction(rule, target, params, env);
				if (ret != null && actionRows[j].isReturnAction())
					return ret;
			}
		}
		return ret;
	}
	
	
	public Object invokeOptimized(Object target, Object[] params, IRuntimeEnv env)
	{
		
		IIntIterator rules = algorithm.checkedRules(target, params, env);
		
		Object ret = null;
		for (; rules.hasNext();)
		{
			int ruleN = rules.nextInt();
			
			for (int j = 0; j < actionRows.length; j++)
			{
				ret = actionRows[j].executeAction(ruleN, target, params, env);
				if (ret != null && actionRows[j].isReturnAction())
					return ret;
			}
			
			
		}
		
		return ret;
		
	}
	
	/**
	 * @param evs
	 */
	protected void makeAlgorithm(IDTConditionEvaluator[] evs)
	{
		algorithm = new DTOptimizedAlgorithm(evs, this);
		algorithm.buildIndex();
	}
	
	
	
	DTOptimizedAlgorithm algorithm;
	

	public IOpenClass getDeclaringClass()
	{
		return header.getDeclaringClass();
	}

	public IMemberMetaInfo getInfo()
	{
//		return header.getInfo();
		return this;
	}

	public String getName()
	{
		return header.getName();
	}

	public IMethodSignature getSignature()
	{
		return header.getSignature();
	}

	public IOpenClass getType()
	{
		return header.getType();
	}

	public boolean isStatic()
	{
		return header.isStatic();
	}

	public IOpenMethod getMethod()
	{
		return this;
	}

	public IDTAction[] getActionRows()
	{
		return actionRows;
	}

	public void setActionRows(IDTAction[] actionRows)
	{
		this.actionRows = actionRows;
	}

	public IDTCondition[] getConditionRows()
	{
		return conditionRows;
	}

	public void setConditionRows(IDTCondition[] conditionRows)
	{
		this.conditionRows = conditionRows;
	}

	public IOpenMethodHeader getHeader()
	{
		return header;
	}

	public void setHeader(IOpenMethodHeader header)
	{
		this.header = header;
	}

	public RuleRow getRuleRow()
	{
		return ruleRow;
	}

	public String getRuleName(int col)
	{
		return ruleRow == null ? "R" + (col + 1) : ruleRow.getRuleName(col);
	}

	public ILogicalTable getRuleTable(int col)
	{
		ILogicalTable bView = (ILogicalTable) tableSyntaxNode.getSubTables().get(
				VIEW_BUSINESS);
		return bView.getLogicalColumn(col + 1);
	}
	
	/**
	 * @return
	 */
	public ILogicalTable getDisplayTable()
	{
		ILogicalTable bView = (ILogicalTable) tableSyntaxNode.getSubTables().get(
				VIEW_BUSINESS);
		return bView.getLogicalColumn(0);
	}
	

	public void setRuleRow(RuleRow ruleRow)
	{
		this.ruleRow = ruleRow;
	}

	public void setColumns(int columns)
	{
		this.columns = columns;
	}

	public void setTableSyntaxNode(TableSyntaxNode tsn)
	{
		this.tableSyntaxNode = tsn;
	}

	public TableSyntaxNode getTableSyntaxNode()
	{
		return tableSyntaxNode;
	}

	/**
	 * @param dependencies
	 */
	public void updateDependency(BindingDependencies dependencies)
	{
		for (int i = 0; i < conditionRows.length; i++)
		{
			((CompositeMethod) conditionRows[i].getMethod())
					.updateDependency(dependencies);
			updateValueDependency((FunctionalRow) conditionRows[i], dependencies);
		}

		for (int i = 0; i < actionRows.length; i++)
		{
			((CompositeMethod) actionRows[i].getMethod())
					.updateDependency(dependencies);
			updateValueDependency((FunctionalRow) actionRows[i], dependencies);

		}

		// header.
	}

	protected void updateValueDependency(FunctionalRow frow,
			BindingDependencies dependencies)
	{
		Object[][] values = frow.getParamValues();

		for (int i = 0; i < values.length; i++)
		{
			if (values[i] == null)
				continue;
			for (int j = 0; j < values[i].length; j++)
			{
				if (values[i][j] instanceof CompositeMethod)
				{
					((CompositeMethod) values[i][j]).updateDependency(dependencies);
				}
			}
		}
	}

	public String toString()
	{
		return getName();
	}


	
	public Object invokeTracedOptimized(Object target, Object[] params, IRuntimeEnv env)
	{

		Tracer t = Tracer.getTracer();
		if (t == null)
			return invokeOptimized(target, params, env);
		Object ret = null;


		try
		{
			DecisionTableTraceObject dtto = new DecisionTableTraceObject(this, params);
			t.push(dtto);	
			IIntIterator rules = algorithm.checkedRules(target, params, env);
		
			for (; rules.hasNext();)
			{
			
			
				int ruleN = rules.nextInt();
			
			
			
				try
				{
					t.push(dtto.traceRule(ruleN));
					for (int j = 0; j < actionRows.length; j++)
					{
						ret = actionRows[j].executeAction(ruleN, target, params, env);
						if (ret != null && actionRows[j].isReturnAction())
						{
							dtto.result = ret;
							return ret;
						}	
							
					}
				} finally
				{
					t.pop();
				}
			
			
			}
			dtto.result = ret;
			return ret;
		}	
		finally
		{
			t.pop();
		}
		
		
		
		
	}
	
	

		public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env)
		{

			Tracer t = Tracer.getTracer();
			if (t == null)
				return invokeStandard(target, params, env);


			try
			{
				DecisionTableTraceObject dtto = new DecisionTableTraceObject(this, params);
				t.push(dtto);	
				
				int nCol = getColumns();
				DecisionTableAlgorithm dta = new DecisionTableAlgorithm(
						conditionRows.length, nCol, conditionRows, target, params, env);
				Object ret = null;
				
				for (int i = 0; i < nCol; i++)
				{
					if (!dta.calcColumn(i))
						continue;
					
					try
					{
						t.push(dtto.traceRule(i));
						for (int j = 0; j < actionRows.length; j++)
						{
							ret = actionRows[j].executeAction(i, target, params, env);
							if (ret != null && actionRows[j].isReturnAction())
							{
								dtto.result = ret;
								return ret;
							}	
								
						}
					} finally
					{
						t.pop();
					}
					
				}
				dtto.result = ret;
				return ret;
			} 
			finally
			{
				t.pop();
			}
		}

	static public class DecisionTableTraceObject extends ITracerObject.SimpleTracerObject
	{
//		DecisionTable dt;
		Object[] params;
		Object result;

//		ArrayList rules = new ArrayList(1);

		public DecisionTableTraceObject(DecisionTable dt, Object[] params)
		{
			super(dt);
			this.params = params;
		}

		public DecisionTable getDT()
		{
			return (DecisionTable)getTraceObject();
		}
		
		public String getDisplayName(int mode)
		{
			StringBuffer buf = new StringBuffer(50);
			buf.append("DT ");
			DecisionTable dt = getDT();
			buf.append(dt.getType().getDisplayName(SHORT)).append(' ');
			buf.append(dt.getName()).append('(');
	
			for (int i = 0; i < params.length; i++)
			{
				if (i > 0)
					buf.append(", ");
				Formatter.format(params[i], INamedThing.SHORT, buf);
			}
			
			buf.append(')');
//			buf.append(MethodUtil.printMethod(getDT(), IMetaInfo.REGULAR, false));
			return buf.toString();
		}
		
		
		


		public String getUri()
		{
			return getDT().getTableSyntaxNode().getUri();
		}

		public RuleTracer traceRule(int i)
		{
			return new RuleTracer(i);
		}
		
		


//		public RuleTracer[] getRuleTracerObjects()
//		{
//			return (RuleTracer[]) rules.toArray(new RuleTracer[0]);
//		}
		
		public class RuleTracer extends ITracerObject.SimpleTracerObject
		{
			int ruleIdx;

			public RuleTracer(int idx)
			{
				super(null);
				this.ruleIdx = idx;
			}

			public String getDisplayName(int mode)
			{
				return "Rule: " + getDT().getRuleName(ruleIdx);
			}



			public String getUri()
			{
				return getRuleTable().getGridTable().getUri();
			}

			public ILogicalTable getRuleTable()
			{
				return getDT().getRuleTable(ruleIdx);
			}

			/* (non-Javadoc)
			 * @see org.openl.util.ITreeElement#getType()
			 */
			public String getType()
			{
				return "rule";
			}
			
			public DecisionTableTraceObject getParentTraceObject()
			{
				return DecisionTableTraceObject.this;
			}
			
		}


		/* (non-Javadoc)
		 * @see org.openl.util.ITreeElement#getType()
		 */
		public String getType()
		{
			return "decisiontable";
		}

	}

	/**
	 * @param header2
	 * @return
	 */
	public static DecisionTable createTable(IOpenMethodHeader header)
	{
		return new DecisionTable(header);
	}

	public BindingDependencies getDependencies()
	{
		BindingDependencies bd = new BindingDependencies();
		updateDependency(bd);
		return bd;
	}

	public ISyntaxNode getSyntaxNode()
	{
		return tableSyntaxNode;
	}


	public String getSourceUrl()
	{
		return tableSyntaxNode.getUri();
	}

	public String getDisplayName(int mode)
	{
		return header.getInfo().getDisplayName(mode);
	}

	public DTOptimizedAlgorithm getAlgorithm() {
		return algorithm;
	}


}
