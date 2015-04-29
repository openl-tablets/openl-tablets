/*
 * Created on Sep 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
package org.openl.rules.dt2;

import java.util.ArrayList;
import java.util.List;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.annotations.Executable;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt2.algorithm.DecisionTableAlgorithmBuilder;
import org.openl.rules.dt2.algorithm.IDecisionTableAlgorithm;
import org.openl.rules.dt2.algorithm.IndexInfo;
import org.openl.rules.dt2.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt2.data.DecisionTableDataType;
import org.openl.rules.dt2.element.ArrayHolder;
import org.openl.rules.dt2.element.FunctionalRow;
import org.openl.rules.dt2.element.IAction;
import org.openl.rules.dt2.element.ICondition;
import org.openl.rules.dt2.element.RuleRow;
import org.openl.rules.dtx.IBaseAction;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.lang.xls.binding.AMethodBasedNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.table.ILogicalTable;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.Invokable;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 * 
 */
@Executable
public class DecisionTable extends ExecutableRulesMethod implements IDecisionTable {

    private IBaseCondition[] conditionRows;
    private IBaseAction[] actionRows;
    /**
     * Optional non-functional row with rule indexes.
     */
    private RuleRow ruleRow;

    private int columns;

    private IDecisionTableAlgorithm algorithm;

    /**
     * Object to invoke current method.
     */
    private Invokable invoker;
    
    private DTInfo dtInfo;
    
    
    

    public DecisionTable(IOpenMethodHeader header, AMethodBasedNode boundNode) {
        super(header, boundNode);
        initProperties(getSyntaxNode().getTableProperties());
    }

    public IBaseAction[] getActionRows() {
        return actionRows;
    }

    public IDecisionTableAlgorithm getAlgorithm() {
        return algorithm;
    }

    public int getColumns() {
        return columns;
    }

    public IBaseCondition[] getConditionRows() {
        return conditionRows;
    }

    public String getDisplayName(int mode) {
        IMemberMetaInfo metaInfo = getHeader().getInfo();
        if (metaInfo != null) {
            return metaInfo.getDisplayName(mode);
        }
        return toString();
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public int getNumberOfRules() {

        if (actionRows.length > 0) {
            return actionRows[0].getNumberOfRules();
        }

        return 0;
    }

    public String getRuleName(int col) {
        return ruleRow == null ? "R" + (col + 1) : ruleRow.getRuleName(col);
    }

    public RuleRow getRuleRow() {
        return ruleRow;
    }

    /**
     * Returns logical table that contains rule column. The column will contain
     * all return, action and condition cells for rule specified by index.
     * 
     * @param ruleIndex Index of rule.
     * @return ILogicalTable that contains rule column.
     */
    public ILogicalTable getRuleTable(int ruleIndex) {
        ILogicalTable dt = actionRows[0].getDecisionTable();
        int starColumn = dt.getWidth() - columns;

        return dt.getColumn(starColumn + ruleIndex);
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public void setActionRows(IAction[] actionRows) {
        this.actionRows = actionRows;
    }

    public void setColumns(int columns) {
        this.columns = columns;
    }

    public void setConditionRows(IBaseCondition[] conditionRows) {
        this.conditionRows = conditionRows;
    }

    public void setRuleRow(RuleRow ruleRow) {
        this.ruleRow = ruleRow;
    }

    public void bindTable(IBaseCondition[] conditionRows, IBaseAction[] actionRows, RuleRow ruleRow, OpenL openl,
            ComponentOpenClass componentOpenClass, IBindingContextDelegator cxtd, int columns) throws Exception {

        this.conditionRows = conditionRows;
        this.actionRows = actionRows;

        if (!cxtd.isExecutionMode()) {
            this.ruleRow = ruleRow;
        }
        this.columns = columns;

        prepare(getHeader(), openl, componentOpenClass, cxtd);
    }

    public BindingDependencies getDependencies() {

        BindingDependencies bindingDependencies = new RulesBindingDependencies();
        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    protected Object innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        if (invoker == null) {
            invoker = new DecisionTableInvoker(this);

        }
        return invoker.invoke(target, params, env);
    }

    /**
     * Check whether execution of decision table should be failed if no rule
     * fired.
     */
    public boolean shouldFailOnMiss() {
        if (getMethodProperties() != null) {
            return (Boolean) getMethodProperties().getPropertyValue("failOnMiss");
        }
        return false;
    }

    protected void makeAlgorithm(IConditionEvaluator[] evs) throws Exception {

        algorithm = new DecisionTableAlgorithmBuilder(new IndexInfo().withTable(this), evs).buildAlgorithm();
        
    }

    private void prepare(IOpenMethodHeader header, OpenL openl, ComponentOpenClass module,
            IBindingContextDelegator bindingContextDelegator) throws Exception {

        IMethodSignature signature = header.getSignature();

        IConditionEvaluator[] evaluators = prepareConditions(openl, module, bindingContextDelegator, signature);

        prepareActions(header, openl, module, bindingContextDelegator, signature);

        makeAlgorithm(evaluators);
    }

    private void prepareActions(IOpenMethodHeader header, OpenL openl, ComponentOpenClass componentOpenClass,
            IBindingContextDelegator bindingContextDelegator, IMethodSignature signature) throws Exception {

        IBindingContextDelegator actionBindingContextDelegator = new ComponentBindingContext(bindingContextDelegator,
                (ComponentOpenClass) getRuleExecutionType(openl));

        for (int i = 0; i < actionRows.length; i++) {
            IOpenClass methodType = actionRows[i].isReturnAction() ? header.getType() : JavaOpenClass.VOID;
            getAction(i).prepareAction(methodType, signature, openl, componentOpenClass,
                    actionBindingContextDelegator, ruleRow, getRuleExecutionType(openl));
        }
    }

    private IConditionEvaluator[] prepareConditions(OpenL openl, ComponentOpenClass componentOpenClass,
            IBindingContextDelegator bindingContextDelegator, IMethodSignature signature) throws Exception {
        IConditionEvaluator[] evaluators = new IConditionEvaluator[conditionRows.length];

        List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

        for (int i = 0; i < conditionRows.length; i++) {
            try {
                evaluators[i] = getCondition(i).prepareCondition(signature, openl, componentOpenClass,
                        bindingContextDelegator, ruleRow);
            } catch (SyntaxNodeException e) {
                errors.add(e);
            } catch (CompositeSyntaxNodeException e) {
                for (SyntaxNodeException syntaxNodeException : e.getErrors()) {
                    errors.add(syntaxNodeException);
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new CompositeSyntaxNodeException("Error:", errors.toArray(new SyntaxNodeException[0]));
        }

        return evaluators;
    }

    @Override
    public String toString() {
        return getName();
    }

    public void updateDependency(BindingDependencies dependencies) {
        if (conditionRows != null) {
            for (IBaseCondition condition : conditionRows) {
                CompositeMethod method = (CompositeMethod) condition.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                updateValueDependency((FunctionalRow) condition, dependencies);
            }
        }

        if (actionRows != null) {
            for (IBaseAction action : actionRows) {
                CompositeMethod method = (CompositeMethod) action.getMethod();
                if (method != null) {
                    method.updateDependency(dependencies);
                }

                updateValueDependency((FunctionalRow) action, dependencies);
            }
        }
    }

    protected void updateValueDependency(FunctionalRow frow, BindingDependencies dependencies) {


    	int len = frow.getNumberOfRules();
    	int np = frow.getNumberOfParams();
    	for (int ruleN = 0; ruleN < len; ruleN++) {

                if (frow.isEmpty(ruleN)) {
                    continue;
                }

                for (int paramIndex = 0; paramIndex < np; paramIndex++) {
                	Object value = frow.getParamValue(paramIndex, ruleN);
                	
                    if (value instanceof CompositeMethod) {
                        ((CompositeMethod) value).updateDependency(dependencies);
                    }
                    else if (value instanceof ArrayHolder) {
						ArrayHolder ah = (ArrayHolder) value;
						ah.updateDependency(dependencies);
					}
                }
            }

    }

    
    public ICondition getCondition(int n)
    {
    	return (ICondition)conditionRows[n];
    }

    public IAction getAction(int n)
    {
    	return (IAction)actionRows[n];
    }
    
    
    IOpenClass ruleExecutionType;

    private synchronized IOpenClass getRuleExecutionType(OpenL openl) {
        if (ruleExecutionType == null) {
            ruleExecutionType = new DecisionTableDataType(this, null, getName() + "Type", openl);
        }
        return ruleExecutionType;
    }

	public DTInfo getDtInfo() {
		return dtInfo;
	}

	public void setDtInfo(DTInfo dtInfo) {
		this.dtInfo = dtInfo;
	}

	@Override
	public int getNumberOfConditions() {
		
		return conditionRows.length;
	}


}