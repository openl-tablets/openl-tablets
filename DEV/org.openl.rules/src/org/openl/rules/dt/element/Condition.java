package org.openl.rules.dt.element;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
import org.openl.binding.impl.IfNode;
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
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.method.ExecutableRulesMethod;
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
import org.openl.types.IOpenMethod;
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

    private static final String FUNCTION_NAME_NODE = "funcname";
    // the words that are not names of anything: the literals and the operators written as words
    private static final Set<String> KEYWORDS = Set.of("true", "false", "null", "and", "or", "not");

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
    private CompositeMethod elseMethod;
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
        var expression = expressionOf(getMethod());
        if (expression == null) {
            return false;
        }
        if (expression instanceof MethodBoundNode call) {
            var inlined = inlineCall(call, signature, openl, bindingContext);
            if (inlined != null && isDependentOnInputParams(inlined)) {
                var inlinedExpression = expressionOf(inlined);
                if (inlinedExpression == null || !splitExpression(inlinedExpression,
                        signature,
                        openl,
                        bindingContext)) {
                    // the expression of the called table holds no static check and is indexed as it is
                    this.indexMethod = inlined;
                }
                return true;
            }
        }
        return splitExpression(expression, signature, openl, bindingContext);
    }

    /**
     * Returns the single expression the method is written of, or {@code null} when it is written of anything else.
     */
    private static IBoundNode expressionOf(CompositeMethod method) {
        var body = method.getMethodBodyBoundNode();
        if (body == null) {
            return null;
        }
        var children = body.getChildren();
        if (children == null || children.length != 1 || children[0] == null || children[0]
                .getChildren() == null || children[0].getChildren().length != 1) {
            return null;
        }
        return children[0].getChildren()[0];
    }

    /**
     * Splits the expression into the check that reads the inputs of the table alone and the part that is indexed.
     */
    private boolean splitExpression(IBoundNode expression,
                                    IMethodSignature signature,
                                    OpenL openl,
                                    IBindingContext bindingContext) {
        IBoundNode left;
        IBoundNode right;
        ISyntaxNode operator;
        boolean conjunction;
        switch (expression) {
            case BinaryOpNodeOr or -> {
                // "a or b or c" reads as "(a or b) or c", and only the leftmost part can be the static one
                var first = or;
                while (first.getLeft() instanceof BinaryOpNodeOr nested) {
                    first = nested;
                }
                left = first.getLeft();
                right = first == or ? or.getRight() : expression;
                operator = first.getSyntaxNode();
                conjunction = false;
            }
            case BinaryOpNodeAnd and -> {
                left = and.getLeft();
                right = and.getRight();
                operator = and.getSyntaxNode();
                conjunction = true;
            }
            case IfNode ifNode -> {
                return optimizeTernaryExpression(ifNode, signature, openl, bindingContext);
            }
            default -> {
                return false;
            }
        }

        var compiledStaticMethod = compileStaticExpression(operator, left, signature, openl);
        if (compiledStaticMethod != null && !isDependentOnInputParams(compiledStaticMethod)) {
            var compiledIndexMethod = compileIndexExpression(operator,
                    right,
                    signature,
                    openl,
                    bindingContext);
            if (compiledIndexMethod != null && isDependentOnInputParams(compiledIndexMethod)) {
                this.staticMethod = compiledStaticMethod;
                this.indexMethod = compiledIndexMethod;
                this.staticConjunction = conjunction;
                return true;
            }
        }
        return false;
    }

    /**
     * Reads a call of a table written of a single expression as that expression, so that the shapes the index
     * understands can be looked for inside it.
     *
     * <p>The arguments of the call take the place of the parameters of the called table. Returns the expression the
     * call stands for, or {@code null} when the call cannot be read this way.
     */
    private CompositeMethod inlineCall(MethodBoundNode call,
                                       IMethodSignature signature,
                                       OpenL openl,
                                       IBindingContext bindingContext) {
        var method = call.getMethodCaller().getMethod();
        var body = singleExpressionBody(method);
        if (body == null) {
            return null;
        }
        var arguments = argumentTexts(call);
        if (arguments == null) {
            return null;
        }
        var inlinedText = substituteParameters(body, method.getSignature(), arguments);
        if (inlinedText == null) {
            return null;
        }
        // the errors of the inlined text are reported at the cell the condition is written in
        var source = new StringSourceCodeModule(inlinedText, getSourceCodeModule(getMethod()).getUri());
        var inlinedMethod = compileIndexSource(source, signature, openl, bindingContext);
        return inlinedMethod == null || callsAnotherTable(inlinedMethod.getMethodBodyBoundNode()) ? null
                : inlinedMethod;
    }

    /**
     * Tells whether the expression calls a table of the project.
     *
     * <p>Such a call is answered by the table the decision table sees, which is not always the one the called table
     * saw, so the expression is not the same expression any more.
     */
    private static boolean callsAnotherTable(IBoundNode node) {
        if (node instanceof MethodBoundNode call && call.getMethodCaller()
                .getMethod() instanceof ExecutableRulesMethod) {
            return true;
        }
        var children = node.getChildren();
        if (children == null) {
            return false;
        }
        for (IBoundNode child : children) {
            if (child != null && callsAnotherTable(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the text of the expression the called table is written of, or {@code null} when the table is written
     * of anything else than a single expression.
     */
    private static String singleExpressionBody(IOpenMethod method) {
        var unwrapped = method;
        while (unwrapped instanceof IOpenMethodWrapper wrapper) {
            unwrapped = wrapper.getDelegate();
        }
        return unwrapped instanceof ExecutableRulesMethod executable ? executable.getSingleExpression() : null;
    }

    /**
     * Returns the text of every argument of the call, in the order the call writes them.
     */
    private static List<String> argumentTexts(MethodBoundNode call) {
        var syntaxNode = call.getSyntaxNode();
        var module = syntaxNode.getModule();
        var children = call.getChildren();
        if (module == null || children == null) {
            return null;
        }
        var sourceCode = module.getCode();
        var info = new TextInfo(sourceCode);
        var texts = new ArrayList<String>();
        for (var i = 0; i < syntaxNode.getNumberOfChildren(); i++) {
            var child = syntaxNode.getChild(i);
            var location = FUNCTION_NAME_NODE.equals(child.getType()) ? null : child.getSourceLocation();
            if (location != null) {
                texts.add(sourceCode.substring(location.getStart().getAbsolutePosition(info),
                        location.getEnd().getAbsolutePosition(info) + 1));
            }
        }
        return texts.size() == children.length ? texts : null;
    }

    /**
     * Writes the arguments of the call in the place of the parameters of the called table.
     *
     * <p>Returns {@code null} when the expression reads a name of its own: read again in the scope of the decision
     * table, such a name may mean something else there. Only the parameters of the table and the functions it calls
     * are allowed.
     */
    private static String substituteParameters(String body, IMethodSignature parameters, List<String> arguments) {
        if (parameters.getNumberOfParameters() != arguments.size()) {
            return null;
        }
        var byName = new HashMap<String, String>();
        for (var i = 0; i < arguments.size(); i++) {
            var name = parameters.getParameterName(i);
            if (name == null || name.isBlank()) {
                return null;
            }
            var argument = arguments.get(i);
            byName.put(name, isSimplePath(argument) ? argument : "(" + argument + ")");
        }
        return writeExpression(body, byName);
    }

    /**
     * Writes the expression with the arguments in the place of the parameters, keeping the text values as they are.
     *
     * <p>Returns {@code null} for an expression that reads a name of its own.
     */
    private static String writeExpression(String body, Map<String, String> arguments) {
        var result = new StringBuilder();
        var position = 0;
        while (position < body.length()) {
            var character = body.charAt(position);
            if (character == '"' || character == '\'') {
                var end = endOfTextValue(body, position);
                result.append(body, position, end);
                position = end;
            } else if (!Character.isJavaIdentifierStart(character)) {
                result.append(character);
                position++;
            } else {
                var end = endOfName(body, position);
                var name = body.substring(position, end);
                if (!writeName(body, position, end, name, arguments, result)) {
                    return null;
                }
                position = end;
            }
        }
        return result.toString();
    }

    /**
     * Tells whether the text is a name, or names joined by dots, and therefore needs no parentheses around it.
     */
    private static boolean isSimplePath(String text) {
        var nameExpected = true;
        for (var i = 0; i < text.length(); i++) {
            var character = text.charAt(i);
            var allowed = nameExpected ? Character.isJavaIdentifierStart(character)
                    : Character.isJavaIdentifierPart(character) || character == '.';
            if (!allowed) {
                return false;
            }
            nameExpected = character == '.';
        }
        return !text.isEmpty() && !nameExpected;
    }

    /**
     * Writes one name of the expression, or the argument that takes its place.
     *
     * <p>Returns {@code false} for a name that stands for something the called table alone knows.
     */
    private static boolean writeName(String body,
                                     int start,
                                     int end,
                                     String name,
                                     Map<String, String> arguments,
                                     StringBuilder result) {
        var argument = arguments.get(name);
        if (argument != null && !isFieldName(body, start)) {
            result.append(argument);
            return true;
        }
        if (argument == null && !isFieldName(body, start) && !isFunctionName(body, end) && !KEYWORDS.contains(name)) {
            return false;
        }
        result.append(name);
        return true;
    }

    private static int endOfName(String body, int start) {
        var end = start;
        while (end < body.length() && Character.isJavaIdentifierPart(body.charAt(end))) {
            end++;
        }
        return end;
    }

    private static int endOfTextValue(String body, int start) {
        var quote = body.charAt(start);
        var position = start + 1;
        while (position < body.length()) {
            var character = body.charAt(position++);
            if (character == '\\') {
                position++;
            } else if (character == quote) {
                break;
            }
        }
        // a text value the last character opens ends with the text itself
        return Math.min(position, body.length());
    }

    private static boolean isFieldName(String body, int start) {
        var position = start - 1;
        while (position >= 0 && Character.isWhitespace(body.charAt(position))) {
            position--;
        }
        return position >= 0 && body.charAt(position) == '.';
    }

    private static boolean isFunctionName(String body, int end) {
        var position = end;
        while (position < body.length() && Character.isWhitespace(body.charAt(position))) {
            position++;
        }
        return position < body.length() && body.charAt(position) == '(';
    }

    /**
     * Splits a condition written as {@code test ? indexed : otherwise}, where the test and the last part read the
     * inputs of the table alone.
     *
     * <p>The test then decides whether the index is asked at all. When it does not hold, the answer of the last
     * part is the answer of the condition for every rule.
     *
     * <p>Returns {@code false} when the condition is written of anything else, and the condition is left as it is.
     */
    private boolean optimizeTernaryExpression(IfNode ifNode,
                                              IMethodSignature signature,
                                              OpenL openl,
                                              IBindingContext bindingContext) {
        if (ifNode.getElseNode() == null) {
            return false;
        }
        var module = ifNode.getSyntaxNode().getModule();
        var questionMark = ifNode.getSyntaxNode().getSourceLocation();
        var elseLocation = ifNode.getElseNode().getSyntaxNode().getSourceLocation();
        if (module == null || questionMark == null || elseLocation == null) {
            return false;
        }
        var sourceCode = module.getCode();
        var info = new TextInfo(sourceCode);
        var questionMarkStart = questionMark.getStart().getAbsolutePosition(info);
        var questionMarkEnd = questionMark.getEnd().getAbsolutePosition(info);
        var colon = sourceCode.lastIndexOf(':', elseLocation.getStart().getAbsolutePosition(info));
        if (colon <= questionMarkEnd) {
            return false;
        }

        var testMethod = compileStaticSource(new SubTextSourceCodeModule(module, 0, questionMarkStart),
                signature,
                openl);
        if (testMethod == null || isDependentOnInputParams(testMethod)) {
            return false;
        }
        var otherwiseMethod = compileStaticSource(new SubTextSourceCodeModule(module, colon + 1), signature, openl);
        if (otherwiseMethod == null || isDependentOnInputParams(otherwiseMethod)) {
            return false;
        }
        var compiledIndexMethod = compileIndexSource(new SubTextSourceCodeModule(module, questionMarkEnd + 1, colon),
                signature,
                openl,
                bindingContext);
        if (compiledIndexMethod == null || !isDependentOnInputParams(compiledIndexMethod)) {
            return false;
        }
        this.staticMethod = testMethod;
        this.elseMethod = otherwiseMethod;
        this.indexMethod = compiledIndexMethod;
        this.staticConjunction = false;
        return true;
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
        if (elseMethod != null) {
            // the test chooses between the lookup and an answer that does not look at the rules at all
            if (Boolean.TRUE.equals(result)) {
                return null;
            }
            return Boolean.TRUE.equals(elseMethod.invoke(null, params, env)) ? Boolean.TRUE : Boolean.FALSE;
        }
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
        if (rightBoundNode instanceof BinaryOpNode || rightBoundNode instanceof BinaryOpNodeOr) {
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
        return compileIndexSource(indexSourceCodeModule, signature, openl, bindingContext);
    }

    private CompositeMethod compileIndexSource(IOpenSourceCodeModule source,
                                               IMethodSignature signature,
                                               OpenL openl,
                                               IBindingContext bindingContext) {
        CompositeMethod indexMethod;
        List<SyntaxNodeException> errors;
        try {
            bindingContext.pushErrors();
            bindingContext.pushMessages();
            indexMethod = super.compileExpressionSource(source, NullOpenClass.the, signature, openl, bindingContext);
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
        return compileStaticSource(staticSourceCodeModule, signature, openl);
    }

    private CompositeMethod compileStaticSource(IOpenSourceCodeModule source,
                                                IMethodSignature signature,
                                                OpenL openl) {
        var returnType = JavaOpenClass.getOpenClass(Boolean.class);
        var staticExprCtx = new BindingContext(openl.getBinder(), returnType, openl);
        var methodHeader = new OpenMethodHeader("run", returnType, signature, null);
        var compiledMethod = OpenLManager.makeMethod(openl, source, methodHeader, staticExprCtx);
        return staticExprCtx.getErrors().length == 0 ? compiledMethod : null;
    }

    @Override
    public IOpenSourceCodeModule getIndexSourceCodeModule() {
        return getSourceCodeModule(getIndexMethod());
    }

    @Override
    public CompositeMethod getIndexMethod() {
        return indexMethod != null ? indexMethod : getMethod();
    }

    @Override
    public void resetOptimizedExpression() {
        this.staticMethod = null;
        this.indexMethod = null;
        this.elseMethod = null;
        this.staticConjunction = false;
    }

    @Override
    public boolean isOptimizedExpression() {
        return staticMethod != null && indexMethod != null;
    }
}
