package org.openl.rules.dt.algorithm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.TypeBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.binding.impl.component.ComponentBindingContext;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.DecisionTableUtils;
import org.openl.rules.dt.IBaseAction;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.ConditionOrActionDirectParameterField;
import org.openl.rules.dt.data.ConditionOrActionParameterField;
import org.openl.rules.dt.data.DecisionTableDataType;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.IAction;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.element.IDecisionRow;
import org.openl.rules.dt.element.RuleRow;
import org.openl.rules.lang.xls.binding.ExpressionIdentifier;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.IParameterDeclaration;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterMethodCaller;
import org.openl.types.impl.SourceCodeMethodCaller;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringUtils;

public class DecisionTableAlgorithmBuilder implements IAlgorithmBuilder {

    /**
     * See also "Using datatype arrays in rules by user defined index" in Reference Guide. Array can be accessed using
     * syntax: drivers[“David”], drivers[“7”].
     *
     * @see org.openl.types.impl.ArrayFieldIndex
     */
    private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile(".+\\[.+]$");

    private IndexInfo baseInfo;
    private final DecisionTable table;
    private IConditionEvaluator[] evaluators;

    private final IOpenMethodHeader header;
    private final OpenL openl;
    private final IMethodSignature signature;
    private final RuleRow ruleRow;

    public DecisionTableAlgorithmBuilder(DecisionTable decisionTable, IOpenMethodHeader header, OpenL openl) {

        this.table = decisionTable;
        this.header = header;
        this.signature = header.getSignature();
        this.openl = openl;
        this.ruleRow = table.getRuleRow();
    }

    private static String cutExpressionRoot(String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        if (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            return v;
        }
        return expression;
    }

    static IOpenClass findExpressionType(IOpenClass type, String expression) {
        StringTokenizer stringTokenizer = new StringTokenizer(expression, ".");
        boolean isFirst = true;
        while (stringTokenizer.hasMoreTokens()) {
            String v = stringTokenizer.nextToken();
            boolean arrayAccess = StringUtils.matches(ARRAY_ACCESS_PATTERN, v);
            if (isFirst) {
                if (arrayAccess) {
                    type = type.getComponentClass();
                }
                isFirst = false;
                continue;
            }
            IOpenField field;
            if (arrayAccess) {
                v = v.substring(0, v.indexOf("["));
            }
            field = type.getField(v);
            type = field.getType();
            if (type.isArray() && arrayAccess) {
                type = type.getComponentClass();
            }
        }
        return type;
    }

    private IDecisionTableAlgorithm buildAlgorithm() {
        if (table.getDtInfo().getNumberHConditions() > 0) {

            IndexInfo vInfo = baseInfo.makeVerticalInfo();
            IndexInfo hInfo = baseInfo.makeHorizontalalInfo();

            IDecisionTableAlgorithm va = new DecisionTableOptimizedAlgorithm(evaluators, table, vInfo);
            IDecisionTableAlgorithm ha = new DecisionTableOptimizedAlgorithm(evaluators, table, hInfo);
            return new TwoDimensionalAlgorithm(va, ha);
        }

        return new DecisionTableOptimizedAlgorithm(evaluators, table, baseInfo);

    }

    private void prepareParams(IDecisionRow decisionRow,
            IBindingContext bindingContext,
            Map<String, Boolean> usedHeaderNames) {
        Boolean v = usedHeaderNames.get(decisionRow.getName());
        if (v == null) {
            usedHeaderNames.put(decisionRow.getName(), Boolean.FALSE);
        } else if (Boolean.FALSE.equals(v)) {
            usedHeaderNames.put(decisionRow.getName(), Boolean.TRUE);
            String columnType = "Condition";
            if (decisionRow instanceof IBaseAction) {
                IBaseAction baseAction = (IBaseAction) decisionRow;
                columnType = baseAction.isReturnAction() ? "Return" : "Action";
            }
            GridCellSourceCodeModule cellSourceCodeModule = new GridCellSourceCodeModule(
                decisionRow.getInfoTable().getSource(),
                bindingContext);
            BindHelper.processError(String.format("%s '%s' is already defined.", columnType, decisionRow.getName()),
                cellSourceCodeModule,
                bindingContext);
        }
        decisionRow.prepareParams(openl, bindingContext);
    }

    private void prepareCondAndActionParams(IBindingContext bindingContext) {
        Map<String, Boolean> usedHeaderNames = new HashMap<>();
        for (IBaseCondition condition : table.getConditionRows()) {
            prepareParams((IDecisionRow) condition, bindingContext, usedHeaderNames);
        }
        for (IBaseAction action : table.getActionRows()) {
            prepareParams((IDecisionRow) action, bindingContext, usedHeaderNames);
        }
    }

    @Override
    public IDecisionTableAlgorithm prepareAndBuildAlgorithm(IBindingContext bindingContext) throws Exception {
        prepareCondAndActionParams(bindingContext);
        DecisionTableDataType ruleExecutionType = new DecisionTableDataType(table,
            table.getName() + "Type",
            openl,
            false);
        evaluators = prepareConditions(ruleExecutionType, bindingContext);
        prepareActions(ruleExecutionType, bindingContext);

        baseInfo = new IndexInfo().withTable(table);
        return buildAlgorithm();
    }

    private void prepareActions(DecisionTableDataType ruleExecutionType,
            IBindingContext bindingContext) throws Exception {
        IBindingContext actionBindingContext = new ComponentBindingContext(bindingContext, ruleExecutionType);
        int nActions = table.getNumberOfActions();
        for (int i = 0; i < nActions; i++) {
            IAction action = table.getAction(i);
            prepareAction(action, actionBindingContext, ruleExecutionType);
        }
    }

    private void prepareAction(IAction action,
            IBindingContext actionBindingContext,
            DecisionTableDataType ruleExecutionType) throws Exception {
        action.prepareAction(table,
            header,
            signature,
            openl,
            actionBindingContext,
            ruleRow,
            ruleExecutionType,
            table.getSyntaxNode());
    }

    private IConditionEvaluator[] prepareConditions(DecisionTableDataType ruleExecutionType,
            IBindingContext bindingContext) {
        IBindingContext conditionBindingContext = new ComponentBindingContext(bindingContext, ruleExecutionType);
        int nConditions = table.getNumberOfConditions();
        final IConditionEvaluator[] evaluators = new IConditionEvaluator[nConditions];
        for (int i = 0; i < nConditions; i++) {
            evaluators[i] = prepareCondition(ruleExecutionType, conditionBindingContext, i);
        }
        return evaluators;
    }

    /**
     * Since version 4.0.2 the Condition can have an optimized form. In this case the Condition Expression(CE) can have
     * type that is different from boolean. For details consult DTOptimizedAlgorithm class
     *
     * The algorithm for Condition Expression parsing will work like this:
     * <p>
     * 1) Compile CE as <code>void</code> expression with all the Condition Parameters(CP). Report errors and return, if
     * any
     * <p>
     * 2) Check if the expression depends on any of the condition parameters, if it does, it is not intended to be
     * optimized (at least not in this version). In this case it has to have <code>boolean</code> type.
     * <p>
     * 3) Try to find possible expression/optimization for the combination of CE and CP types. See DTOptimizedAlgorithm
     * for the full set of available optimizations. If not found - raise exception.
     * <p>
     * 4) Attach, expression/optimization to the Condition; change the expression header to remove CP, because they are
     * not needed.
     *
     * @see DecisionTableOptimizedAlgorithm
     * @since 4.0.2
     */

    private IConditionEvaluator prepareCondition(DecisionTableDataType ruleExecutionType,
            IBindingContext bindingContext,
            int index) {
        ICondition condition = table.getCondition(index);

        try {
            condition.prepare(table,
                NullOpenClass.the,
                signature,
                openl,
                bindingContext,
                ruleRow,
                ruleExecutionType,
                table.getSyntaxNode());
        } catch (Exception e) {
            BindHelper.processError(e, table.getSyntaxNode(), bindingContext);
            return DefaultConditionEvaluator.INSTANCE;
        }
        IBoundMethodNode methodNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
        if (methodNode == null) {
            // method defined with error
            return DefaultConditionEvaluator.INSTANCE;
        }
        condition.setConditionParametersUsed(checkConditionParameterUsedInExpression(condition));
        condition.setRuleIdOrRuleNameUsed(checkRuleIdOrRuleNameInExpression(condition));
        condition.setDependentOnOtherColumnsParams(checkOtherColumnParametersInExpression((Condition) condition));

        IOpenSourceCodeModule source = methodNode.getSyntaxNode().getModule();
        if (StringUtils.isEmpty(source.getCode())) {
            BindHelper.processError("Cannot execute empty expression.", source, bindingContext);
            return DefaultConditionEvaluator.INSTANCE;
        }

        // tested in TypeInExpressionTest
        //
        IBoundNode[] children = methodNode.getChildren();
        if (children != null && children.length == 1 && children[0].getChildren() != null && children[0]
            .getChildren().length > 0 && children[0].getChildren()[0] instanceof TypeBoundNode) {
            String message = String.format("Cannot execute expression with only type definition '%s'.",
                source.getCode());
            BindHelper.processError(message, source, bindingContext);
            return DefaultConditionEvaluator.INSTANCE;
        }
        IOpenClass methodType = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode().getType();
        if (condition.isDependentOnOtherColumnsParams()) {
            condition.setConditionEvaluator(DefaultConditionEvaluator.INSTANCE);
            if (!JavaOpenClass.BOOLEAN.equals(methodType) && !JavaOpenClass.getOpenClass(Boolean.class)
                .equals(methodType)) {
                if (condition.getParams().length != 1) {
                    BindHelper.processError(
                        "Condition expression must return a boolean type if it uses condition parameters.",
                        source,
                        bindingContext);
                    return DefaultConditionEvaluator.INSTANCE;
                } else {
                    IOpenCast openCast = bindingContext.getCast(methodType, condition.getParams()[0].getType());
                    if (openCast.isImplicit()) {
                        condition.setComparisonCast(openCast);
                    }
                }
            }
            return DefaultConditionEvaluator.INSTANCE;
        }

        if (condition.isDependentOnInputParams() || condition.isRuleIdOrRuleNameUsed()) {
            if (!JavaOpenClass.BOOLEAN.equals(methodType) && !JavaOpenClass.getOpenClass(Boolean.class)
                .equals(methodType)) {
                BindHelper.processError(
                    "Condition expression must return a boolean type if it uses condition parameters.",
                    source,
                    bindingContext);
                return DefaultConditionEvaluator.INSTANCE;
            }

            IConditionEvaluator conditionEvaluator = DependentParametersOptimizedAlgorithm
                .makeEvaluator(condition, signature, bindingContext);

            if (conditionEvaluator != null) {
                condition.setConditionEvaluator(conditionEvaluator);
                IMethodCaller evaluator = makeOptimizedConditionMethodEvaluator(condition,
                    signature,
                    conditionEvaluator.getOptimizedSourceCode());
                condition.setEvaluator(evaluator);
                if (evaluator == null) {
                    condition.setEvaluator(makeDependentParamsIndexedConditionMethodEvaluator(condition,
                        signature,
                        conditionEvaluator.getOptimizedSourceCode()));
                }
            } else {
                conditionEvaluator = DefaultConditionEvaluator.INSTANCE;
                condition.setConditionEvaluator(conditionEvaluator);
            }
            return conditionEvaluator;
        } else {
            IConditionEvaluator dtcev = DecisionTableOptimizedAlgorithm
                .makeEvaluator(condition, methodType, bindingContext);
            condition.setEvaluator(makeOptimizedConditionMethodEvaluator(condition, signature));
            condition.setConditionEvaluator(dtcev);
            return dtcev;
        }
    }

    private boolean checkOtherColumnParametersInExpression(Condition condition) {
        BindingDependencies dependencies = new RulesBindingDependencies();
        condition.getMethod().updateDependency(dependencies);
        for (IOpenField field : dependencies.getFieldsMap().values()) {
            field = Condition.getLocalField(field);
            if (field instanceof ConditionOrActionParameterField || field instanceof ConditionOrActionDirectParameterField) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkConditionParameterUsedInExpression(ICondition condition) {
        List<ExpressionIdentifier> identifiers = DecisionTableUtils.extractIdentifiers(condition);
        for (IParameterDeclaration condParam : condition.getParams()) {
            if (identifiers.stream()
                .anyMatch(identifierNode -> condParam.getName() != null && condParam.getName()
                    .equalsIgnoreCase(identifierNode.getIdentifier()))) {
                return true;
            }
        }
        if (identifiers.stream()
            .anyMatch(identifierNode -> Objects.equals(SpreadsheetStructureBuilder.DOLLAR_SIGN + condition.getName(),
                identifierNode.getIdentifier()))) {
            return true;
        }
        return false;
    }

    private static boolean checkRuleIdOrRuleNameInExpression(ICondition condition) {
        List<ExpressionIdentifier> identifiers = DecisionTableUtils.extractIdentifiers(condition);
        return identifiers.stream()
            .anyMatch(e -> "$Rule".equals(e.getIdentifier()) || "$RuleId".equals(e.getIdentifier()));
    }

    private IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition, IMethodSignature signature) {
        return makeOptimizedConditionMethodEvaluator(condition,
            signature,
            DecisionTableUtils.getConditionSourceCode(condition));
    }

    private static IMethodCaller makeOptimizedConditionMethodEvaluator(ICondition condition,
            IMethodSignature signature,
            String code) {
        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            String pname = signature.getParameterName(i);
            if (pname.equals(code)) {
                return new ParameterMethodCaller(condition.getMethod(), i);
            }
        }
        return null;
    }

    private static IMethodCaller makeDependentParamsIndexedConditionMethodEvaluator(ICondition condition,
            IMethodSignature signature,
            String optimizedCode) {
        String v = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();
        if (optimizedCode != null && !optimizedCode.equals(v)) {
            String p = cutExpressionRoot(optimizedCode);
            for (int i = 0; i < signature.getNumberOfParameters(); i++) {
                String pname = signature.getParameterName(i);
                if (pname.equals(p)) {
                    IOpenClass type = findExpressionType(signature.getParameterType(i), optimizedCode);
                    return new SourceCodeMethodCaller(signature, type, optimizedCode);
                }
            }
        }
        return null;
    }
}
