package org.openl.rules.dt.element;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.openl.OpenL;
import org.openl.binding.BindingDependencies;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundMethodNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.BinaryOpNode;
import org.openl.binding.impl.BinaryOpNodeOr;
import org.openl.binding.impl.BindingContext;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.engine.OpenLManager;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.dt.DTScale;
import org.openl.rules.dt.DecisionTableRuntimePool;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.data.RuleExecutionObject;
import org.openl.rules.helpers.CharRange;
import org.openl.rules.helpers.DateRange;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.INumberRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.helpers.NumberUtils;
import org.openl.rules.helpers.StringRange;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.GridTableUtils;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.source.impl.SubTextSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.MessageUtils;
import org.openl.util.text.TextInfo;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    private IMethodCaller evaluator;
    private IConditionEvaluator conditionEvaluator;
    private IOpenSourceCodeModule userDefinedOpenSourceCodeModule;
    private boolean conditionParametersUsed;
    private boolean ruleIdOrRuleNameUsed;
    private boolean dependentOnOtherColumnsParams;
    private IOpenCast comparisonCast;
    private CompositeMethod staticMethod;
    private CompositeMethod indexMethod;

    public Condition(String name, int row, ILogicalTable table, DTScale.RowScale scale) {
        super(name, row, table, scale);
    }

    @Override
    public IParameterDeclaration[] getParams() {
        IParameterDeclaration[] params = super.getParams();
        return params == null ? IParameterDeclaration.EMPTY : params;
    }

    @Override
    public IConditionEvaluator getConditionEvaluator() {
        return conditionEvaluator;
    }

    @Override
    public void setConditionEvaluator(IConditionEvaluator conditionEvaluator) {
        this.conditionEvaluator = conditionEvaluator;
    }

    @Override
    public boolean isAction() {
        return false;
    }

    @Override
    public boolean isCondition() {
        return true;
    }

    @Override
    public IMethodCaller getEvaluator() {
        return evaluator == null ? getMethod() : evaluator;
    }

    @Override
    public void setEvaluator(IMethodCaller evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public DecisionValue calculateCondition(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {
        if (target instanceof IDynamicObject) {
            target = new RuleExecutionObject(ruleExecutionType, (IDynamicObject) target, ruleN);
        }

        if (isEmpty(ruleN)) {
            return DecisionValue.NxA_VALUE;
        }

        if (conditionParametersUsed || ruleIdOrRuleNameUsed || dependentOnOtherColumnsParams) {
            return makeDecision(ruleN, target, dtParams, env);
        } else {
            /*
             * IMPORTANT NOTE: Performance optimization when condition parameter is not used in the expression. No need
             * to execute expression per each ruleNumber cause the result will be always the same.
             */
            DecisionTableRuntimePool runtimePool = (DecisionTableRuntimePool) env.getLocalFrame()[0];
            DecisionValue decisionValue = (DecisionValue) runtimePool.getConditionExecutionResult(getName());
            if (decisionValue == null) {
                decisionValue = makeDecision(ruleN, target, dtParams, env);
                runtimePool.pushConditionExecutionResultToPool(getName(), decisionValue);
            }
            return decisionValue;
        }
    }

    @Override
    public void setComparisonCast(IOpenCast comparisonCast) {
        this.comparisonCast = comparisonCast;
    }

    private DecisionValue makeDecision(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {
        Object[] params = mergeParams(target, dtParams, env, ruleN);
        Object result = getMethod().invoke(target, params, env);
        if (comparisonCast != null) {
            result = comparisonCast.convert(result);
            return Objects.equals(result, params[params.length - 1]) ? DecisionValue.TRUE_VALUE
                    : DecisionValue.FALSE_VALUE;
        }
        return Boolean.TRUE.equals(result) ? DecisionValue.TRUE_VALUE : DecisionValue.FALSE_VALUE;
    }

    public static IOpenField getLocalField(IOpenField f) {
        if (f instanceof ILocalVar) {
            return f;
        }

        if (f instanceof OpenFieldDelegator) {
            OpenFieldDelegator d = (OpenFieldDelegator) f;
            return d.getDelegate();
        }
        return f;
    }

    @Override
    public boolean isDependentOnInputParams() {
        IParameterDeclaration[] params = getParams();

        BindingDependencies dependencies = new RulesBindingDependencies();
        getMethod().updateDependency(dependencies);

        for (IOpenField field : dependencies.getFieldsMap().values()) {
            field = getLocalField(field);
            if (field instanceof ILocalVar) {
                for (IParameterDeclaration param : params) {
                    if (Objects.equals(field.getName(), param.getName())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @Override
    public IOpenSourceCodeModule getUserDefinedExpressionSource() {
        if (userDefinedOpenSourceCodeModule == null) {
            return getSourceCodeModule();
        }
        return userDefinedOpenSourceCodeModule;
    }

    @Override
    protected IOpenSourceCodeModule getExpressionSource(TableSyntaxNode tableSyntaxNode,
                                                        IMethodSignature signature,
                                                        IOpenClass methodParamType,
                                                        IOpenClass declaringClass,
                                                        OpenL openl,
                                                        IBindingContext bindingContext) throws Exception {

        if (!GridTableUtils.isSingleCellTable(getCodeTable())) {
            ILogicalTable redundantRow = getCodeTable().getRow(1); // Bind error to the redundant expression definition
            IOpenSourceCodeModule errorSrc = new GridCellSourceCodeModule(redundantRow.getSource(), bindingContext);
            throw SyntaxNodeExceptionUtils
                    .createError(MessageUtils.getConditionMultipleExpressionErrorMessage(getName()), errorSrc);
        }

        IOpenSourceCodeModule source = super.getExpressionSource(tableSyntaxNode,
                signature,
                methodParamType,
                declaringClass,
                openl,
                bindingContext);

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            if (signature.getParameterName(i).equals(source.getCode())) {
                userDefinedOpenSourceCodeModule = source;
                prepareParams(declaringClass, signature, methodParamType, source, openl, bindingContext);
                if (params.length == 1) {
                    if (params[0].getType()
                            .isArray() && params[0].getType().getComponentClass().getInstanceClass() != null) {
                        IOpenClass inputType = signature.getParameterType(i);
                        ConditionCasts conditionCasts = ConditionHelper
                                .findConditionCasts(params[0].getType().getComponentClass(), inputType, bindingContext);
                        if (conditionCasts.isCastToConditionTypeExists() || (conditionCasts
                                .isCastToInputTypeExists() && !inputType.isArray())) {
                            return !hasFormulas() ? source
                                    : new StringSourceCodeModule(
                                    getContainsInArrayExpression(tableSyntaxNode,
                                            source,
                                            signature.getParameterType(i),
                                            params[0],
                                            conditionCasts,
                                            bindingContext),
                                    source.getUri()); // build an expression for condition (must be
                            // the same as indexed variant)
                        }
                    }

                    if (isRangeExpression(signature.getParameterType(i), params[0].getType())) {
                        return !hasFormulas() ? source
                                : new StringSourceCodeModule(
                                getRangeExpression(tableSyntaxNode,
                                        source,
                                        signature.getParameterType(i),
                                        params[0],
                                        bindingContext),
                                source.getUri()); // build an expression for condition (must be the
                        // same as indexed variant)
                    }

                    return !hasFormulas() && !(params[0].getType().isArray() && signature.getParameterType(i)
                            .isArray()) ? source
                            : new StringSourceCodeModule(source.getCode() + " == " + params[0].getName(),
                            source.getUri()); // build an expression if default evaluator is used
                } else if (params.length == 2) {
                    return !hasFormulas() ? source
                            : new StringSourceCodeModule(params[0].getName() + "<=" + source
                            .getCode() + " and " + source.getCode() + "<" + params[1].getName(),
                            source.getUri()); // build an expression if default evaluator is used
                }
            }
        }
        return source;

    }

    private String getContainsInArrayExpression(TableSyntaxNode tableSyntaxNode,
                                                IOpenSourceCodeModule source,
                                                IOpenClass methodType,
                                                IParameterDeclaration param,
                                                ConditionCasts conditionCasts,
                                                IBindingContext bindingContext) {
        if (Objects.equals(param.getType().getComponentClass(), methodType)) {
            return String.format("contains(%s, %s)", param.getName(), source.getCode());
        }
        if (conditionCasts.isCastToConditionTypeExists()) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                    "PERFORMANCE: Condition '%s' uses additional type casting " + "from '%s' to '%s' in calculation time for each table row.",
                    getName(),
                    methodType.getName(),
                    param.getType().getComponentClass().getName()), tableSyntaxNode));
            return String.format("contains(%s, (%s) %s)",
                    param.getName(),
                    param.getType().getComponentClass().getName(),
                    source.getCode());
        } else if (conditionCasts.isCastToInputTypeExists()) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                    "PERFORMANCE: Condition '%s' uses additional type casting " + "from '%s' to '%s' in calculation time for each table row.",
                    getName(),
                    param.getType().getComponentClass().getInstanceClass().getTypeName(),
                    methodType.getName()), tableSyntaxNode));
            return String.format("contains((%s[]) %s, %s)", methodType.getName(), param.getName(), source.getCode());
        } else {
            throw new IllegalStateException("It should not happen.");
        }
    }

    private static boolean isIntRangeType(IOpenClass type) {
        return IntRange.class == type.getInstanceClass();
    }

    private String getRangeExpression(TableSyntaxNode tableSyntaxNode,
                                      IOpenSourceCodeModule source,
                                      IOpenClass methodType,
                                      IParameterDeclaration param,
                                      IBindingContext bindingContext) {
        if (isIntRangeType(param.getType()) && NumberUtils.isFloatPointType(methodType.getInstanceClass())) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(String.format(
                    "PERFORMANCE: Condition '%s' uses additional type casting " + "from '%s' to '%s' in calculation time for each table row.",
                    getName(),
                    param.getType().getName(),
                    DoubleRange.class.getTypeName()), tableSyntaxNode));
        }
        return String.format("contains(%s, %s)", param.getName(), source.getCode());
    }

    private static boolean isRangeExpression(IOpenClass methodType, IOpenClass paramType) {
        if (ClassUtils.isAssignable(paramType.getInstanceClass(), INumberRange.class) && ClassUtils
                .isAssignable(methodType.getInstanceClass(), Number.class)) {
            return true;
        }
        if (ClassUtils.isAssignable(paramType.getInstanceClass(), INumberRange.class) && methodType.getInstanceClass()
                .isPrimitive() && char.class != methodType.getInstanceClass()) {
            return true;
        }
        if (ClassUtils.isAssignable(paramType.getInstanceClass(), DateRange.class) && ClassUtils
                .isAssignable(methodType.getInstanceClass(), Date.class)) {
            return true;
        }
        if (ClassUtils.isAssignable(paramType.getInstanceClass(),
                CharRange.class) && (ClassUtils.isAssignable(methodType.getInstanceClass(),
                Character.class) || char.class == methodType.getInstanceClass())) {
            return true;
        }
        return ClassUtils.isAssignable(paramType.getInstanceClass(), StringRange.class) && ClassUtils
                .isAssignable(methodType.getInstanceClass(), CharSequence.class);
    }

    @Override
    public int getNumberOfEmptyRules(int paramIndex) {
        if (storage != null) {
            return storage[paramIndex].getInfo().getNumberOfSpaces();
        }
        return 0;
    }

    @Override
    public void setConditionParametersUsed(boolean conditionParametersUsed) {
        this.conditionParametersUsed = conditionParametersUsed;
    }

    @Override
    public boolean isRuleIdOrRuleNameUsed() {
        return ruleIdOrRuleNameUsed;
    }

    @Override
    public void setRuleIdOrRuleNameUsed(boolean ruleIdOrRuleNameUsed) {
        this.ruleIdOrRuleNameUsed = ruleIdOrRuleNameUsed;
    }

    @Override
    public boolean isDependentOnOtherColumnsParams() {
        return dependentOnOtherColumnsParams;
    }

    public void setDependentOnOtherColumnsParams(boolean dependentOnOtherColumnsParams) {
        this.dependentOnOtherColumnsParams = dependentOnOtherColumnsParams;
    }

    @Override
    protected CompositeMethod compileExpressionSource(IOpenSourceCodeModule source,
                                                      IOpenClass methodType,
                                                      IMethodSignature signature,
                                                      OpenL openl,
                                                      IBindingContext bindingContext) {
        var originalExpr = super.compileExpressionSource(source, methodType, signature, openl, bindingContext);
        if (bindingContext.getErrors().length == 0) {
            optimizeExpression(originalExpr.getMethodBodyBoundNode(), methodType, signature, openl, bindingContext);
        }
        return originalExpr;
    }

    private void optimizeExpression(IBoundMethodNode originalExprBoundNode,
                                    IOpenClass methodType,
                                    IMethodSignature signature,
                                    OpenL openl,
                                    IBindingContext bindingContext) {
        if (originalExprBoundNode == null) {
            return;
        }
        var children = originalExprBoundNode.getChildren();
        if (children != null && children.length == 1 &&
                children[0] != null && children[0].getChildren() != null &&
                children[0].getChildren().length == 1 &&
                children[0].getChildren()[0] instanceof BinaryOpNodeOr) {
            var binaryOpNodeOr = (BinaryOpNodeOr) children[0].getChildren()[0];

            var staticMethod = compileStaticExpression(binaryOpNodeOr, signature, openl);
            if (staticMethod != null) {
                var indexMethod = compileIndexExpression(binaryOpNodeOr, methodType, signature, openl, bindingContext);
                if (indexMethod != null) {
                    this.staticMethod = staticMethod;
                    this.indexMethod = indexMethod;
                }
            }
        }
    }

    private CompositeMethod compileIndexExpression(BinaryOpNodeOr binaryOpNodeOr, IOpenClass methodType, IMethodSignature signature, OpenL openl, IBindingContext bindingContext) {
        var rightBoundNode = binaryOpNodeOr.getRight();
        IOpenSourceCodeModule indexSourceCodeModule;
        if (rightBoundNode instanceof BinaryOpNode) {
            var module = binaryOpNodeOr.getSyntaxNode().getModule();
            var location = binaryOpNodeOr.getSyntaxNode().getSourceLocation();
            var sourceCode = module.getCode();
            indexSourceCodeModule = new SubTextSourceCodeModule(module,
                    location.getEnd().getAbsolutePosition(new TextInfo(sourceCode)) + 1);
        } else if (rightBoundNode instanceof MethodBoundNode) {
            indexSourceCodeModule = rightBoundNode.getSyntaxNode().getSourceCodeModule();
        } else {
            return null;
        }

        CompositeMethod indexMethod;
        List<SyntaxNodeException> errors;
        try {
            bindingContext.pushErrors();
            bindingContext.pushMessages();
            indexMethod = super.compileExpressionSource(indexSourceCodeModule,
                    methodType,
                    signature,
                    openl,
                    bindingContext);
        } finally {
            errors = bindingContext.popErrors();
            bindingContext.popMessages();
        }
        return errors.isEmpty() ? indexMethod : null;
    }

    private CompositeMethod compileStaticExpression(BinaryOpNodeOr binaryOpNodeOr, IMethodSignature signature, OpenL openl) {
        var rightBoundNode = binaryOpNodeOr.getLeft();
        IOpenSourceCodeModule staticSourceCodeModule;
        if (rightBoundNode instanceof BinaryOpNode) {
            var module = binaryOpNodeOr.getSyntaxNode().getModule();
            var location = binaryOpNodeOr.getSyntaxNode().getSourceLocation();
            var sourceCode = module.getCode();
            staticSourceCodeModule = new SubTextSourceCodeModule(module,
                    0,
                    location.getStart().getAbsolutePosition(new TextInfo(sourceCode)));
        } else if (rightBoundNode instanceof MethodBoundNode
                || rightBoundNode instanceof LiteralBoundNode
                || rightBoundNode instanceof FieldBoundNode) {
            staticSourceCodeModule = rightBoundNode.getSyntaxNode().getSourceCodeModule();
        } else {
            return null;
        }

        var returnType = JavaOpenClass.getOpenClass(Boolean.class);
        var staticExprCtx = new BindingContext(openl.getBinder(), returnType, openl);
        OpenMethodHeader methodHeader = new OpenMethodHeader("run", returnType, signature, null);
        var compiledMethod = OpenLManager.makeMethod(openl,
                staticSourceCodeModule,
                methodHeader,
                staticExprCtx);
        return staticExprCtx.getErrors().length == 0 ? compiledMethod : null;
    }

    @Override
    public CompositeMethod getStaticMethod() {
        return staticMethod;
    }

    @Override
    public IOpenSourceCodeModule getIndexSourceCodeModule() {
        return Optional.ofNullable(indexMethod)
                .map(this::getSourceCodeModule)
                .orElseGet(this::getSourceCodeModule);
    }

    @Override
    public CompositeMethod getIndexMethod() {
        return Optional.ofNullable(indexMethod)
                .orElseGet(this::getMethod);
    }
}
