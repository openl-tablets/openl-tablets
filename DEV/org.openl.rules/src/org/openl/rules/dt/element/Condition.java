package org.openl.rules.dt.element;

import java.util.Date;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.ILocalVar;
import org.openl.binding.impl.BinaryOpNode;
import org.openl.binding.impl.BinaryOpNodeAnd;
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
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IDynamicObject;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IParameterDeclaration;
import org.openl.types.Invokable;
import org.openl.types.NullOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.OpenFieldDelegator;
import org.openl.types.impl.OpenMethodHeader;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.util.MessageUtils;
import org.openl.util.text.TextInfo;
import org.openl.vm.IRuntimeEnv;

public class Condition extends FunctionalRow implements ICondition {

    @Setter
    private Invokable evaluator;
    @Getter
    @Setter
    private IConditionEvaluator conditionEvaluator;
    private IOpenSourceCodeModule userDefinedOpenSourceCodeModule;
    @Setter
    private boolean conditionParametersUsed;
    @Getter
    @Setter
    private boolean ruleIdOrRuleNameUsed;
    @Getter
    @Setter
    private boolean dependentOnOtherColumnsParams;
    @Setter
    private IOpenCast comparisonCast;
    @Getter
    private CompositeMethod staticMethod;
    private CompositeMethod indexMethod;
    private boolean staticConjunction;

    public Condition(String name, int row, ILogicalTable table, DTScale.RowScale scale) {
        super(name, row, table, scale);
    }

    @Override
    public IParameterDeclaration[] getParams() {
        var params = super.getParams();
        return params == null ? IParameterDeclaration.EMPTY : params;
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
    public Invokable getEvaluator() {
        return evaluator == null ? getMethod() : evaluator;
    }

    @Override
    public DecisionValue calculateCondition(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {
        if (target instanceof IDynamicObject object) {
            target = new RuleExecutionObject(ruleExecutionType, object, ruleN);
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
            var runtimePool = (DecisionTableRuntimePool) env.getLocalFrame()[0];
            var decisionValue = (DecisionValue) runtimePool.getConditionExecutionResult(getName());
            if (decisionValue == null) {
                decisionValue = makeDecision(ruleN, target, dtParams, env);
                runtimePool.pushConditionExecutionResultToPool(getName(), decisionValue);
            }
            return decisionValue;
        }
    }

    private DecisionValue makeDecision(int ruleN, Object target, Object[] dtParams, IRuntimeEnv env) {
        var params = mergeParams(target, dtParams, env, ruleN);
        var result = getMethod().invoke(target, params, env);
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

        if (f instanceof OpenFieldDelegator d) {
            return d.getDelegate();
        }
        return f;
    }

    @Override
    public boolean isDependentOnInputParams() {
        return isDependentOnInputParams(getMethod());
    }

    private boolean isDependentOnInputParams(CompositeMethod method) {
        var params = getParams();

        var dependencies = new RulesBindingDependencies();
        method.updateDependency(dependencies);

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
            var redundantRow = getCodeTable().getRow(1); // Bind error to the redundant expression definition
            var errorSrc = new GridCellSourceCodeModule(redundantRow.getSource(), bindingContext);
            throw SyntaxNodeExceptionUtils
                    .createError(MessageUtils.getConditionMultipleExpressionErrorMessage(getName()), errorSrc);
        }

        var source = super.getExpressionSource(tableSyntaxNode,
                signature,
                methodParamType,
                declaringClass,
                openl,
                bindingContext);

        for (var i = 0; i < signature.getNumberOfParameters(); i++) {
            if (signature.getParameterName(i).equals(source.getCode())) {
                userDefinedOpenSourceCodeModule = source;
                prepareParams(declaringClass, signature, methodParamType, source, openl, bindingContext);
                if (params.length == 1) {
                    if (params[0].getType()
                            .isArray() && params[0].getType().getComponentClass().getInstanceClass() != null) {
                        var inputType = signature.getParameterType(i);
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
            return "contains(%s, %s)".formatted(param.getName(), source.getCode());
        }
        if (conditionCasts.isCastToConditionTypeExists()) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage("""
                    PERFORMANCE: Condition '%s' uses additional type casting \
                    from '%s' to '%s' in calculation time for each table row.""".formatted(
                    getName(),
                    methodType.getName(),
                    param.getType().getComponentClass().getName()), tableSyntaxNode));
            return "contains(%s, (%s) %s)".formatted(
                    param.getName(),
                    param.getType().getComponentClass().getName(),
                    source.getCode());
        } else if (conditionCasts.isCastToInputTypeExists()) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage("""
                    PERFORMANCE: Condition '%s' uses additional type casting \
                    from '%s' to '%s' in calculation time for each table row.""".formatted(
                    getName(),
                    param.getType().getComponentClass().getInstanceClass().getTypeName(),
                    methodType.getName()), tableSyntaxNode));
            return "contains((%s[]) %s, %s)".formatted(methodType.getName(), param.getName(), source.getCode());
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
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage("""
                    PERFORMANCE: Condition '%s' uses additional type casting \
                    from '%s' to '%s' in calculation time for each table row.""".formatted(
                    getName(),
                    param.getType().getName(),
                    DoubleRange.class.getTypeName()), tableSyntaxNode));
        }
        return "contains(%s, %s)".formatted(param.getName(), source.getCode());
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
    public boolean optimizeExpression(IMethodSignature signature,
                                      OpenL openl,
                                      IBindingContext bindingContext) {
        var originalExprBoundNode = getMethod().getMethodBodyBoundNode();
        if (originalExprBoundNode == null) {
            return false;
        }
        var children = originalExprBoundNode.getChildren();
        if (children == null || children.length != 1 || children[0] == null || children[0]
                .getChildren() == null || children[0].getChildren().length != 1) {
            return false;
        }
        var expression = children[0].getChildren()[0];

        IBoundNode left;
        IBoundNode right;
        boolean conjunction;
        if (expression instanceof BinaryOpNodeOr or) {
            left = or.getLeft();
            right = or.getRight();
            conjunction = false;
        } else if (expression instanceof BinaryOpNodeAnd and) {
            left = and.getLeft();
            right = and.getRight();
            conjunction = true;
        } else {
            return false;
        }

        var staticMethod = compileStaticExpression(expression.getSyntaxNode(), left, signature, openl);
        if (staticMethod != null && !isDependentOnInputParams(staticMethod)) {
            var indexMethod = compileIndexExpression(expression.getSyntaxNode(),
                    right,
                    signature,
                    openl,
                    bindingContext);
            if (indexMethod != null && isDependentOnInputParams(indexMethod)) {
                this.staticMethod = staticMethod;
                this.indexMethod = indexMethod;
                this.staticConjunction = conjunction;
                return true;
            }
        }
        return false;
    }

    /**
     * Tells what the static part of the condition has decided for the rules with a filled cell.
     *
     * <p>{@code TRUE} means that every rule of the index matches, {@code FALSE} that none of them does, and
     * {@code null} that the index has to be asked for the value.
     */
    @Override
    public Boolean evaluateStaticDecision(Object[] params, IRuntimeEnv env) {
        var result = (Boolean) staticMethod.invoke(null, params, env);
        if (staticConjunction) {
            // the condition holds only when both parts do, so anything but true leaves no rule to match
            return Boolean.TRUE.equals(result) ? null : Boolean.FALSE;
        }
        return Boolean.TRUE.equals(result) ? Boolean.TRUE : null;
    }

    private CompositeMethod compileIndexExpression(ISyntaxNode operator,
                                                  IBoundNode rightBoundNode,
                                                  IMethodSignature signature,
                                                  OpenL openl,
                                                  IBindingContext bindingContext) {
        IOpenSourceCodeModule indexSourceCodeModule;
        if (rightBoundNode instanceof BinaryOpNode) {
            var module = operator.getModule();
            var location = operator.getSourceLocation();
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
                    NullOpenClass.the,
                    signature,
                    openl,
                    bindingContext);
        } finally {
            errors = bindingContext.popErrors();
            bindingContext.popMessages();
        }
        return errors.isEmpty() ? indexMethod : null;
    }

    private CompositeMethod compileStaticExpression(ISyntaxNode operator,
                                                   IBoundNode leftBoundNode,
                                                   IMethodSignature signature,
                                                   OpenL openl) {
        IOpenSourceCodeModule staticSourceCodeModule;
        if (leftBoundNode instanceof BinaryOpNode) {
            var module = operator.getModule();
            var location = operator.getSourceLocation();
            var sourceCode = module.getCode();
            staticSourceCodeModule = new SubTextSourceCodeModule(module,
                    0,
                    location.getStart().getAbsolutePosition(new TextInfo(sourceCode)));
        } else if (leftBoundNode instanceof MethodBoundNode
                || leftBoundNode instanceof LiteralBoundNode
                || leftBoundNode instanceof FieldBoundNode) {
            staticSourceCodeModule = leftBoundNode.getSyntaxNode().getSourceCodeModule();
        } else {
            return null;
        }

        var returnType = JavaOpenClass.getOpenClass(Boolean.class);
        var staticExprCtx = new BindingContext(openl.getBinder(), returnType, openl);
        var methodHeader = new OpenMethodHeader("run", returnType, signature, null);
        var compiledMethod = OpenLManager.makeMethod(openl,
                staticSourceCodeModule,
                methodHeader,
                staticExprCtx);
        return staticExprCtx.getErrors().length == 0 ? compiledMethod : null;
    }

    @Override
    public IOpenSourceCodeModule getIndexSourceCodeModule() {
        return getSourceCodeModule(isOptimizedExpression() ? indexMethod : getMethod());
    }

    @Override
    public CompositeMethod getIndexMethod() {
        return isOptimizedExpression() ? indexMethod : getMethod();
    }

    @Override
    public void resetOptimizedExpression() {
        this.staticMethod = null;
        this.indexMethod = null;
        this.staticConjunction = false;
    }

    @Override
    public boolean isOptimizedExpression() {
        return staticMethod != null && indexMethod != null;
    }
}
