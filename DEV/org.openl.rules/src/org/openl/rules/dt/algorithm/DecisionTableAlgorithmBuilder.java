package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openl.OpenL;
import org.openl.binding.IBindingContextDelegator;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.binding.impl.component.ComponentOpenClass;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dtx.IBaseCondition;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.impl.SourceCodeMethodCaller;
import org.openl.types.java.JavaOpenClass;

public class DecisionTableAlgorithmBuilder implements IAlgorithmBuilder {

	protected IndexInfo baseInfo;
	protected DecisionTable table;
	protected IConditionEvaluator[] evaluators;

	IOpenMethodHeader header;
	OpenL openl;
	ComponentOpenClass componentOpenClass;
	ComponentOpenClass module;
	IBindingContextDelegator bindingContextDelegator;
	IMethodSignature signature;
	private RuleRow ruleRow;

	public DecisionTableAlgorithmBuilder(DecisionTable decisionTable,
			IOpenMethodHeader header, OpenL openl, ComponentOpenClass module,
			IBindingContextDelegator bindingContextDelegator) {

		this.table = decisionTable;
		this.header = header;
		this.signature = header.getSignature();
		this.openl = openl;
		this.module = module;
		this.bindingContextDelegator = bindingContextDelegator;
		this.ruleRow = table.getRuleRow();
	}

	protected ARuleIndex buildIndex(IndexInfo info) throws SyntaxNodeException {

		int first = info.fromCondition;
		IBaseCondition[] cc = table.getConditionRows();

		if (cc.length <= first || first > info.toCondition)
			return null;

		ICondition firstCondition = (ICondition) cc[first];

		if (!canIndex(evaluators[first], firstCondition))
			return null;

		ARuleIndex indexRoot = evaluators[first].makeIndex(firstCondition,
				info.makeRuleIterator());
		// indexRoot.setHasMetaInfo(saveRulesMetaInfo);

		indexNodes(indexRoot, first + 1, info);

		return indexRoot;
	}

	private void indexNodes(ARuleIndex index, int condN, IndexInfo info) {

		if (index == null || condN > info.toCondition)
			return;

		if (!canIndex(evaluators[condN], table.getCondition(condN))) {
			return;
		}

		Iterator<DecisionTableRuleNode> iter = index.nodes();
		while (iter.hasNext()) {
			DecisionTableRuleNode node = iter.next();
			indexNode(node, condN, info);
		}
		indexNode(index.getEmptyOrFormulaNodes(), condN, info);
	}

	private void indexNode(DecisionTableRuleNode node, int condN, IndexInfo info) {

		ARuleIndex nodeIndex = evaluators[condN].makeIndex(
				table.getCondition(condN), node.getRulesIterator());
		// node.setSaveRulesMetaInfo(saveRulesMetaInfo);
		node.setNextIndex(nodeIndex);

		indexNodes(nodeIndex, condN + 1, info);
	}

	protected boolean canIndex(IConditionEvaluator evaluator,
			ICondition condition) {
		return evaluator.isIndexed() && !condition.hasFormulasInStorage();
	}

	public IDecisionTableAlgorithm buildAlgorithm() throws SyntaxNodeException {
		if (isTwoDimensional(table)) {
			IDecisionTableAlgorithm va = makeVerticalAlgorithm();
			IDecisionTableAlgorithm ha = makeHorizontalAlgorithm();
			TwoDimensionalAlgorithm twoD = new TwoDimensionalAlgorithm(va, ha);
			return twoD;
		}

		return makeFullAlgorithm();

	}

	protected IDecisionTableAlgorithm makeHorizontalAlgorithm()
			throws SyntaxNodeException {

		IndexInfo hInfo = baseInfo.makeHorizontalalInfo();

		ARuleIndex index = buildIndex(hInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(
				evaluators, table, hInfo, index);

		return alg;
	}

	protected IDecisionTableAlgorithm makeFullAlgorithm()
			throws SyntaxNodeException {
		ARuleIndex index = buildIndex(baseInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(
				evaluators, table, baseInfo, index);

		return alg;
	}

	protected IDecisionTableAlgorithm makeVerticalAlgorithm()
			throws SyntaxNodeException {

		IndexInfo vInfo = baseInfo.makeVerticalInfo();

		ARuleIndex index = buildIndex(vInfo);
		DecisionTableOptimizedAlgorithm alg = new DecisionTableOptimizedAlgorithm(
				evaluators, table, vInfo, index);

		return alg;
	}

	protected boolean isTwoDimensional(DecisionTable table2) {
		return table.getDtInfo().getNumberHConditions() > 0;
	}

	@Override
	public IDecisionTableAlgorithm prepareAndBuildAlgorithm() throws Exception {
		evaluators = prepareConditions();

		prepareActions();

		baseInfo = new IndexInfo().withTable(table);
		return buildAlgorithm();
	}

	protected DecisionTableDataType getRuleExecutionType(OpenL openl) {
		return new DecisionTableDataType(table, null, table.getName() + "Type",
				openl);
	}

	protected void prepareActions() throws Exception {

		DecisionTableDataType ruleExecutionType = getRuleExecutionType(openl);

		IBindingContextDelegator actionBindingContextDelegator = new ComponentBindingContext(
				bindingContextDelegator, ruleExecutionType);

		int nActions = table.getNumberOfActions();
		for (int i = 0; i < nActions; i++) {
			IAction action = table.getAction(i);
			prepareAction(action, actionBindingContextDelegator,
					ruleExecutionType);
			// getAction(i).prepareAction(methodType, signature, openl,
			// componentOpenClass,
			// actionBindingContextDelegator, ruleRow, ruleExecutionType);
		}
	}

	protected void prepareAction(IAction action,
			IBindingContextDelegator actionBindingContextDelegator,
			DecisionTableDataType ruleExecutionType) throws Exception {
		IOpenClass methodType = action.isReturnAction() ? header.getType()
				: JavaOpenClass.VOID;

		action.prepareAction(methodType, signature, openl, componentOpenClass,
				actionBindingContextDelegator, ruleRow, ruleExecutionType);

	}

	protected IConditionEvaluator[] prepareConditions() throws Exception {
		int nConditions = table.getNumberOfConditions();
		IConditionEvaluator[] evaluators = new IConditionEvaluator[nConditions];

		List<SyntaxNodeException> errors = new ArrayList<SyntaxNodeException>();

		for (int i = 0; i < nConditions; i++) {
			try {
				ICondition condition = table.getCondition(i);
				evaluators[i] = prepareCondition(condition);
			} catch (SyntaxNodeException e) {
				errors.add(e);
			} catch (CompositeSyntaxNodeException e) {
				for (SyntaxNodeException syntaxNodeException : e.getErrors()) {
					errors.add(syntaxNodeException);
				}
			}
		}

		if (!errors.isEmpty()) {
			throw new CompositeSyntaxNodeException("Error:",
					errors.toArray(new SyntaxNodeException[0]));
		}

		return evaluators;
	}

	/**
	 * Since version 4.0.2 the Condition can have an optimized form. In this
	 * case the Condition Expression(CE) can have type that is different from
	 * boolean. For details consult DTOptimizedAlgorithm class
	 * 
	 * The algorithm for Condition Expression parsing will work like this:
	 * <p>
	 * 1) Compile CE as <code>void</code> expression with all the Condition
	 * Parameters(CP). Report errors and return, if any
	 * <p>
	 * 2) Check if the expression depends on any of the condition parameters, if
	 * it does, it is not intended to be optimized (at least not in this
	 * version). In this case it has to have <code>boolean</code> type.
	 * <p>
	 * 3) Try to find possible expression/optimization for the combination of CE
	 * and CP types. See DTOptimizedAlgorithm for the full set of available
	 * optimizations. If not found - raise exception.
	 * <p>
	 * 4) Attach, expression/optimization to the Condition; change the
	 * expression header to remove CP, because they are not needed.
	 * 
	 * @see DecisionTableOptimizedAlgorithm
	 * @since 4.0.2
	 */

	protected IConditionEvaluator prepareCondition(ICondition condition)
			throws Exception {
		// return condition.prepareCondition(signature, openl,
		// componentOpenClass, bindingContextDelegator, ruleRow);

		condition.prepare(NullOpenClass.the, signature, openl,
				componentOpenClass, bindingContextDelegator, ruleRow);
		IBoundMethodNode methodNode = ((CompositeMethod) condition.getMethod())
				.getMethodBodyBoundNode();
		IOpenSourceCodeModule source = methodNode.getSyntaxNode().getModule();

		if (StringUtils.isEmpty(source.getCode())) {
			throw SyntaxNodeExceptionUtils.createError(
					"Cannot execute empty expression", source);
		}

		// tested in TypeInExpressionTest
		//
		if (methodNode.getChildren().length == 1
				&& methodNode.getChildren()[0].getChildren()[0] instanceof TypeBoundNode) {
			String message = String.format(
					"Cannot execute expression with only type definition %s",
					source.getCode());
			throw SyntaxNodeExceptionUtils.createError(message, source);
		}

		IOpenClass methodType = ((CompositeMethod) condition.getMethod())
				.getBodyType();

		IConditionEvaluator conditionEvaluator = condition
				.getConditionEvaluator();
		IMethodCaller evaluator = condition.getEvaluator();
		if (condition.isDependentOnAnyParams()) {
			if (methodType != JavaOpenClass.BOOLEAN
					&& methodType != JavaOpenClass.getOpenClass(Boolean.class)) {
				throw SyntaxNodeExceptionUtils
						.createError(
								"Condition must have boolean type if it depends on it's parameters",
								source);
			}

			condition
					.setConditionEvaluator(conditionEvaluator = DependentParametersOptimizedAlgorithm
							.makeEvaluator(condition, signature,
									bindingContextDelegator));

			if (conditionEvaluator != null) {
				condition
						.setEvaluator(evaluator = makeOptimizedConditionMethodEvaluator(
								condition, signature,
								conditionEvaluator.getOptimizedSourceCode()));
				if (evaluator == null) {
					condition
							.setEvaluator(evaluator = makeDependentParamsIndexedConditionMethodEvaluator(
									condition, signature,
									conditionEvaluator.getOptimizedSourceCode()));
				}
				return conditionEvaluator;
			}

			condition
					.setConditionEvaluator(conditionEvaluator = new DefaultConditionEvaluator());
			return conditionEvaluator;
		}

		IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm
				.makeEvaluator(condition, methodType, bindingContextDelegator);

		condition
				.setEvaluator(evaluator = makeOptimizedConditionMethodEvaluator(
						condition, signature));

		condition.setConditionEvaluator(conditionEvaluator = dtcev);
		return conditionEvaluator;

	}

	protected IMethodCaller makeOptimizedConditionMethodEvaluator(
			ICondition condition, IMethodSignature signature) {
		String code = ((CompositeMethod) condition.getMethod())
				.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
		return makeOptimizedConditionMethodEvaluator(condition, signature, code);
	}

	protected IMethodCaller makeOptimizedConditionMethodEvaluator(
			ICondition condition, IMethodSignature signature, String code) {
		for (int i = 0; i < signature.getNumberOfParameters(); i++) {
			String pname = signature.getParameterName(i);
			if (pname.equals(code)) {
				return new ParameterMethodCaller(condition.getMethod(), i);
			}
		}
		return null;
	}

	protected IMethodCaller makeDependentParamsIndexedConditionMethodEvaluator(
			ICondition condition, IMethodSignature signature,
			String optimizedCode) {
		String v = ((CompositeMethod) condition.getMethod())
				.getMethodBodyBoundNode().getSyntaxNode().getModule().getCode();
		if (optimizedCode != null && !optimizedCode.equals(v)) {
			String p = ExpressionTypeUtils.cutExpressionRoot(optimizedCode);
			for (int i = 0; i < signature.getNumberOfParameters(); i++) {
				String pname = signature.getParameterName(i);
				if (pname.equals(p)) {
					IOpenClass type = ExpressionTypeUtils.findExpressionType(
							signature.getParameterType(i), optimizedCode);
					return new SourceCodeMethodCaller(signature, type,
							optimizedCode);
				}
			}
		}
		return null;
	}

}
