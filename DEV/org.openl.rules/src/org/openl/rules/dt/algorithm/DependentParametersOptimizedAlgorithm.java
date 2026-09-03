package org.openl.rules.dt.algorithm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BinaryOpNode;
import org.openl.binding.impl.BinaryOpNodeAnd;
import org.openl.binding.impl.BinaryOpNodeOr;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.binding.impl.IndexNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.CombinedRangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ConditionParameter;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluatorV2;
import org.openl.rules.dt.algorithm.evaluator.ContainsInInputArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluatorV2;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.SingleRangeIndexEvaluator;
import org.openl.rules.dt.element.Condition;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ConditionHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.ITypeAdaptor;
import org.openl.rules.helpers.RulesUtils;
import org.openl.rules.range.Range;
import org.openl.rules.util.Arrays;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.ParameterDeclaration;

/**
 * Builds an index for a condition whose expression mentions the condition column parameters.
 *
 * <p>The following expressions are recognized, where {@code input} is a decision table argument or a path that
 * starts from one, and {@code column} is the condition column parameter:
 *
 * <ul>
 * <li>{@code input == column} - an equals index over the column values;</li>
 * <li>{@code contains(column, input)} - an equals index over the values of the column array;</li>
 * <li>{@code contains(input, column)} - an equals index over the column values, looked up once per element of the
 * input array;</li>
 * <li>{@code input >= column} and the other comparisons, with one or two boundary columns - a range index.</li>
 * </ul>
 *
 * <p>Any other expression returns no evaluator, and the condition is evaluated row by row.
 *
 * @see DecisionTableOptimizedAlgorithm
 */
class DependentParametersOptimizedAlgorithm {

    static IConditionEvaluator makeEvaluator(ICondition condition,
                                             IMethodSignature signature,
                                             IBindingContext bindingContext) {
        return makeEvaluator(condition, signature, bindingContext, new ICondition[]{condition});
    }

    /**
     * Builds an evaluator for the condition, looking the column parameters up in every column of the table.
     */
    static IConditionEvaluator makeEvaluator(ICondition condition,
                                             IMethodSignature signature,
                                             IBindingContext bindingContext,
                                             ICondition[] conditions) {
        if (condition.hasFormulas() || condition.isRuleIdOrRuleNameUsed()) {
            return null;
        }

        EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(condition,
                signature,
                bindingContext,
                conditions);

        if (evaluatorFactory == null) {
            return null;
        }

        if (condition.getParams().length == 1) {
            return makeOneParamEvaluator(condition, bindingContext, evaluatorFactory);
        } else if (condition.getParams().length == 2) {
            return makeTwoParamEvaluator(condition, bindingContext, evaluatorFactory);
        } else {
            return null;
        }
    }

    private static IConditionEvaluator makeTwoParamEvaluator(ICondition condition,
                                                             IBindingContext bindingContext,
                                                             EvaluatorFactory evaluatorFactory) {
        var expressionType = evaluatorFactory.getExpressionType();
        if (expressionType == null) {
            // Fall back to default evaluator
            return null;
        }
        var params = condition.getParams();
        var conditionParamType0 = params[0].getType();
        var conditionParamType1 = params[1].getType();

        if (conditionParamType0.equals(conditionParamType1)) {
            ConditionCasts conditionCasts = ConditionHelper
                    .findConditionCasts(conditionParamType0, expressionType, bindingContext);

            if (!conditionCasts.atLeastOneExists()) {
                var message = "Cannot convert from '%s' to '%s'. Incompatible types comparison in '%s' condition.".formatted(
                        conditionParamType0.getName(),
                        expressionType.getName(),
                        condition.getName());
                BindHelper.processError(message, condition.getUserDefinedExpressionSource(), bindingContext);
                return null;
            }

            var adaptor = getRangeAdaptor(evaluatorFactory,
                    conditionParamType0,
                    expressionType,
                    conditionCasts);

            if (adaptor == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            var rix = new CombinedRangeIndexEvaluator(
                    (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor,
                    2,
                    ConditionHelper.getConditionCastsWithNoCasts());

            rix.setOptimizedSourceCode(evaluatorFactory.getExpression());

            return rix;
        }
        return null;
    }

    private static IConditionEvaluator makeOneParamEvaluator(ICondition condition,
                                                             IBindingContext bindingContext,
                                                             EvaluatorFactory evaluatorFactory) {
        var expressionType = evaluatorFactory.getExpressionType();
        if (expressionType == null) {
            // Fall back to default evaluator
            return null;
        }
        var params = condition.getParams();
        var conditionParamType = params[0].getType();

        if (evaluatorFactory instanceof OneParameterContainsInInputArrayFactory factory) {
            var values = factory instanceof ContainsInInputArrayChainFactory chain ? chain.getValues()
                    : List.<ConditionParameter>of();
            var valueType = values.isEmpty() ? conditionParamType
                    : values.get(0).condition().getParams()[values.get(0).index()].getType();
            var evaluator = makeContainsInInputArrayEvaluator(expressionType, valueType, values, bindingContext);
            if (evaluator != null) {
                evaluator.setOptimizedSourceCode(factory.getExpression());
            }
            return evaluator;
        }

        if (evaluatorFactory instanceof OneParameterContainsInFactory factory) {
            var aggregateInfo = conditionParamType.getAggregateInfo();
            if (aggregateInfo.isAggregate(conditionParamType)) {
                var componentType = aggregateInfo.getComponentType(conditionParamType);
                if (Range.class.isAssignableFrom(componentType.getInstanceClass())) {
                    // indexing of range arrays is not support right now. Default condition evaluator must be used
                    return null;
                }
                ConditionCasts aggregateConditionCasts = ConditionHelper.findConditionCasts(componentType, expressionType, bindingContext);
                if (aggregateConditionCasts.isCastToConditionTypeExists() || aggregateConditionCasts
                        .isCastToInputTypeExists() && !expressionType.isArray()) {
                    return condition.getNumberOfEmptyRules(0) > 1 || condition.getStaticMethod() != null
                            ? new OneParameterContainsInArrayIndexedEvaluatorV2(
                            factory,
                            aggregateConditionCasts)
                            : new OneParameterContainsInArrayIndexedEvaluator(
                            factory,
                            aggregateConditionCasts);
                }
            }
            return null;
        }

        ConditionCasts conditionCasts = ConditionHelper
                .findConditionCasts(conditionParamType, expressionType, bindingContext);

        if (!conditionCasts.atLeastOneExists()) {
            var message = "Cannot convert from '%s' to '%s'. Incompatible types comparison in '%s' condition.".formatted(
                    conditionParamType.getName(),
                    expressionType.getName(),
                    condition.getName());

            BindHelper.processError(message, condition.getUserDefinedExpressionSource(), bindingContext);
            return null;
        }

        if (evaluatorFactory instanceof OneParameterEqualsFactory factory) {
            if (!conditionParamType.isArray() && !expressionType.isArray()) {
                return condition.getNumberOfEmptyRules(0) > 1 || condition.getStaticMethod() != null
                        ? new OneParameterEqualsIndexedEvaluatorV2(
                        factory,
                        conditionCasts)
                        : new OneParameterEqualsIndexedEvaluator(
                        factory,
                        conditionCasts);
            }
        } else {
            var adaptor = getRangeAdaptor(evaluatorFactory,
                    conditionParamType,
                    expressionType,
                    conditionCasts);

            if (adaptor == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            var rix = new SingleRangeIndexEvaluator(
                    (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor,
                    conditionCasts);
            rix.setOptimizedSourceCode(evaluatorFactory.getExpression());
            return rix;
        }
        return null;
    }

    /**
     * Builds an evaluator for {@code contains(inputArray, columnValue)}, where the array is passed to the decision
     * table and a single column value is looked up in it.
     *
     * <p>Returns {@code null} when the expression is not of that shape, so that the default evaluator is used.
     */
    private static ContainsInInputArrayIndexedEvaluator makeContainsInInputArrayEvaluator(
            IOpenClass inputArrayType,
            IOpenClass conditionParamType,
            List<ConditionParameter> values,
            IBindingContext bindingContext) {
        if (!inputArrayType.isArray() || conditionParamType.isArray()) {
            // only a single column value is looked up in an array of inputs
            return null;
        }
        var valueClass = conditionParamType.getInstanceClass();
        if (valueClass == null || Range.class.isAssignableFrom(valueClass)) {
            // a column declared with an error has no type, and ranges are indexed by the range evaluators
            return null;
        }
        var componentType = inputArrayType.getComponentClass();
        ConditionCasts conditionCasts = ConditionHelper
                .findConditionCasts(conditionParamType, componentType, bindingContext);
        if (conditionCasts.isCastToConditionTypeExists() || conditionCasts
                .isCastToInputTypeExists() && !componentType.isArray()) {
            return new ContainsInInputArrayIndexedEvaluator(conditionCasts, values);
        }
        return null;
    }

    private static IRangeAdaptor<?, ? extends Comparable<?>> getRangeAdaptor(EvaluatorFactory evaluatorFactory,
                                                                             IOpenClass paramType,
                                                                             IOpenClass expressionType,
                                                                             ConditionCasts conditionCasts) {
        Class<?> typeClass = conditionCasts.isCastToInputTypeExists() ? expressionType.getInstanceClass()
                : paramType.getInstanceClass();

        if (typeClass == String.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.STRING, conditionCasts);
        }

        if (typeClass == byte.class || typeClass == Byte.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BYTE, conditionCasts);
        }

        if (typeClass == short.class || typeClass == Short.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.SHORT, conditionCasts);
        }

        if (typeClass == int.class || typeClass == Integer.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.INT, conditionCasts);
        }

        if (typeClass == long.class || typeClass == Long.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.LONG, conditionCasts);
        }

        if (typeClass == float.class || typeClass == Float.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.FLOAT, conditionCasts);
        }

        if (typeClass == double.class || typeClass == Double.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DOUBLE, conditionCasts);
        }

        if (typeClass == BigInteger.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGINTEGER, conditionCasts);
        }

        if (typeClass == BigDecimal.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGDECIMAL, conditionCasts);
        }

        if (typeClass == Date.class) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DATE, conditionCasts);
        }

        return null;
    }

    private static String buildFieldName(IndexNode indexNode, IBindingContext bindingContext) {
        String value = null;
        var children = indexNode.getChildren();
        if (children != null && children.length == 1 && children[0] instanceof LiteralBoundNode literalBoundNode) {
            if ("literal.string".equals(literalBoundNode.getSyntaxNode().getType())) {
                value = "[\"" + literalBoundNode.getValue().toString() + "\"]";
            } else {
                value = "[" + literalBoundNode.getValue().toString() + "]";
            }
        } else {
            BindHelper.processError("Cannot parse array index.", indexNode.getSyntaxNode(), bindingContext);
            return value;
        }

        if (indexNode.getTargetNode() != null) {
            if (indexNode.getTargetNode() instanceof FieldBoundNode) {
                return buildFieldName((FieldBoundNode) indexNode.getTargetNode(), bindingContext) + value;
            }
            if (indexNode.getTargetNode() instanceof IndexNode) {
                return value + buildFieldName((IndexNode) indexNode.getTargetNode(), bindingContext);
            }
            BindHelper.processError("Cannot parse array index.", indexNode.getSyntaxNode(), bindingContext);
        }
        return value;
    }

    private static String buildFieldName(FieldBoundNode field, IBindingContext bindingContext) {
        var value = field.getFieldName();
        if (field.getTargetNode() != null) {
            if (field.getTargetNode() instanceof FieldBoundNode) {
                return buildFieldName((FieldBoundNode) field.getTargetNode(), bindingContext) + "." + value;
            }
            if (field.getTargetNode() instanceof IndexNode) {
                return buildFieldName((IndexNode) field.getTargetNode(), bindingContext) + "." + value;
            }
            return null;
        }
        return value;
    }

    private static Triple<String, RelationType, String> parseMethodBoundExpression(MethodBoundNode methodBoundNode,
                                                                                   IBindingContext ctx) {
        var children = methodBoundNode.getChildren();
        if (children != null && children.length == 2 && children[0] instanceof FieldBoundNode fieldBoundNode0 && children[1] instanceof FieldBoundNode fieldBoundNode1) {
            RelationType relationType;
            if (isContainsMethod(methodBoundNode)) {
                relationType = RelationType.IN;
            } else {
                return null;
            }
            return Triple.of(buildFieldName(fieldBoundNode0, ctx), relationType, buildFieldName(fieldBoundNode1, ctx));
        }

        return null;
    }

    /**
     * Checks that the call is the built-in {@code contains}, and not a function of the same name declared by the
     * project. A project function may answer anything, so its calls are left to the default evaluator.
     */
    private static boolean isContainsMethod(MethodBoundNode methodBoundNode) {
        var method = methodBoundNode.getMethodCaller().getMethod();
        if (!"contains".equals(method.getName())) {
            return false;
        }
        var declaringClass = method.getDeclaringClass();
        return declaringClass != null && RulesUtils.class == declaringClass.getInstanceClass();
    }

    private static Triple<String, RelationType, String> parseBinaryOpExpression(BinaryOpNode binaryOpNode,
                                                                                IBindingContext bindingContext) {
        var children = binaryOpNode.getChildren();
        if (children != null && children.length == 2 && children[0] instanceof FieldBoundNode fieldBoundNode0 && children[1] instanceof FieldBoundNode fieldBoundNode1) {
            RelationType relationType;
            if (binaryOpNode.getSyntaxNode()
                    .getType()
                    .endsWith("ge") && !binaryOpNode.getSyntaxNode().getType().endsWith("string_ge")) {
                relationType = RelationType.GE;
            } else if (binaryOpNode.getSyntaxNode()
                    .getType()
                    .endsWith("gt") && !binaryOpNode.getSyntaxNode().getType().endsWith("string_gt")) {
                relationType = RelationType.GT;
            } else if (binaryOpNode.getSyntaxNode()
                    .getType()
                    .endsWith("le") && !binaryOpNode.getSyntaxNode().getType().endsWith("string_le")) {
                relationType = RelationType.LE;
            } else if (binaryOpNode.getSyntaxNode()
                    .getType()
                    .endsWith("lt") && !binaryOpNode.getSyntaxNode().getType().endsWith("string_lt")) {
                relationType = RelationType.LT;
            } else if (binaryOpNode.getSyntaxNode()
                    .getType()
                    .endsWith("eq") && !binaryOpNode.getSyntaxNode().getType().endsWith("string_eq")) {
                relationType = RelationType.EQ;
            } else {
                return null;
            }

            return Triple.of(buildFieldName(fieldBoundNode0, bindingContext),
                    relationType,
                    buildFieldName(fieldBoundNode1, bindingContext));
        }
        return null;
    }

    private static Triple<String, RelationType, String> oneParameterExpressionParse(ICondition condition,
                                                                                    IBindingContext bindingContext) {
        if (condition.getIndexMethod() != null) {
            var boundNode = condition.getIndexMethod().getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode blockNode) {
                var children = blockNode.getChildren();
                if (children != null && children.length == 1 && children[0] instanceof BlockNode node) {
                    blockNode = node;
                    children = blockNode.getChildren();
                    if (children.length == 1) {
                        if (children[0] instanceof BinaryOpNode binaryOpNode) {
                            return parseBinaryOpExpression(binaryOpNode, bindingContext);
                        } else if (children[0] instanceof MethodBoundNode methodBoundNode) {
                            return parseMethodBoundExpression(methodBoundNode, bindingContext);
                        }
                    }
                }
            }
            return null;
        }
        throw new IllegalStateException("Condition method is not an instance of CompositeMethod.");
    }

    /**
     * Reads a condition written as {@code isEmpty(column) or contains(column, input)}.
     *
     * <p>The check holds for a rule whose cell is empty, and such a rule matches anything anyway, so the whole
     * condition is the lookup alone. An array of a filled cell always holds a value, so the check cannot hold
     * for it.
     *
     * <p>Returns the parsed lookup, or {@code null} when the expression has another shape.
     */
    private static Triple<String, RelationType, String> emptyOrContainsParse(ICondition condition,
                                                                             IBindingContext bindingContext) {
        var expression = indexExpressionNode(condition);
        if (!(expression instanceof BinaryOpNodeOr or) || !(or
                .getLeft() instanceof MethodBoundNode isEmptyCall) || !(or
                .getRight() instanceof MethodBoundNode containsCall)) {
            return null;
        }
        var arguments = isEmptyCall.getChildren();
        if (!isEmptyMethod(isEmptyCall) || arguments == null || arguments.length != 1 || !(arguments[0] instanceof FieldBoundNode field)) {
            return null;
        }
        var parsed = parseMethodBoundExpression(containsCall, bindingContext);
        if (parsed == null || parsed.getMiddle() != RelationType.IN) {
            return null;
        }
        var columnName = buildFieldName(field, bindingContext);
        // the emptiness must be asked about the very array the lookup searches, and it must be the column
        return columnName != null && columnName.equals(parsed.getLeft()) && columnName
                .equals(condition.getParams()[0].getName()) ? parsed : null;
    }

    /**
     * Checks that the call is the built-in {@code isEmpty}, and not a function of the same name declared by the
     * project.
     */
    private static boolean isEmptyMethod(MethodBoundNode methodBoundNode) {
        var method = methodBoundNode.getMethodCaller().getMethod();
        if (!"isEmpty".equals(method.getName())) {
            return false;
        }
        var declaringClass = method.getDeclaringClass();
        return declaringClass != null && Arrays.class == declaringClass.getInstanceClass();
    }

    /**
     * Reads a condition written as several {@code contains} calls joined by {@code or}, such as
     * {@code contains(codes, code) or contains(codes, linkedCode)}.
     *
     * <p>Returns the parsed calls, or {@code null} when the expression is not such a chain.
     */
    private static List<Triple<String, RelationType, String>> containsChainParse(ICondition condition,
                                                                                 IBindingContext bindingContext) {
        var expression = indexExpressionNode(condition);
        if (!(expression instanceof BinaryOpNodeOr)) {
            return null;
        }
        var operands = new ArrayList<IBoundNode>();
        flattenOr(expression, operands);
        var result = new ArrayList<Triple<String, RelationType, String>>(operands.size());
        for (IBoundNode operand : operands) {
            if (!(operand instanceof MethodBoundNode methodBoundNode)) {
                return null;
            }
            var parsed = parseMethodBoundExpression(methodBoundNode, bindingContext);
            if (parsed == null || parsed.getMiddle() != RelationType.IN) {
                return null;
            }
            result.add(parsed);
        }
        return result;
    }

    private static void flattenOr(IBoundNode node, List<IBoundNode> operands) {
        if (node instanceof BinaryOpNodeOr or) {
            flattenOr(or.getLeft(), operands);
            flattenOr(or.getRight(), operands);
        } else {
            operands.add(node);
        }
    }

    private static IBoundNode indexExpressionNode(ICondition condition) {
        if (condition.getIndexMethod() == null) {
            throw new IllegalStateException("Condition method is not an instance of CompositeMethod.");
        }
        var boundNode = condition.getIndexMethod().getMethodBodyBoundNode();
        if (boundNode instanceof BlockNode blockNode) {
            var children = blockNode.getChildren();
            if (children != null && children.length == 1 && children[0] instanceof BlockNode node) {
                children = node.getChildren();
                if (children.length == 1) {
                    return children[0];
                }
            }
        }
        return null;
    }

    /**
     * Describes a chain of {@code contains} calls over the same array of inputs.
     *
     * <p>Every call must look up a column value of the table, and all of them must be of the same type. The
     * values may come from other columns, as {@code contains(codes, code) or contains(codes, linkedCode)} does
     * when {@code linkedCode} is declared by another column.
     */
    private static EvaluatorFactory makeContainsInInputArrayChainFactory(
            List<Triple<String, RelationType, String>> chain,
            IMethodSignature signature,
            ICondition[] conditions) {
        var inputPath = chain.get(0).getLeft();
        IParameterDeclaration signatureParam = getParameter(inputPath, signature);
        if (signatureParam == null || findColumnParameter(inputPath, conditions) != null) {
            // the array is searched in, so it must come from the inputs of the table and not from a column
            return null;
        }
        var values = new ArrayList<ConditionParameter>(chain.size());
        IOpenClass valueType = null;
        for (Triple<String, RelationType, String> parsed : chain) {
            if (!inputPath.equals(parsed.getLeft()) || getParameter(parsed.getRight(), signature) != null) {
                return null;
            }
            var value = findColumnParameter(parsed.getRight(), conditions);
            if (value == null) {
                return null;
            }
            var type = value.condition().getParams()[value.index()].getType();
            if (valueType != null && !valueType.equals(type)) {
                return null;
            }
            valueType = type;
            values.add(value);
        }
        return new ContainsInInputArrayChainFactory(signatureParam,
                getOrBuildParameterPath(inputPath, signatureParam),
                values);
    }

    /**
     * Finds the column that declares the parameter.
     *
     * <p>The columns of the table are prepared one by one, so a parameter of a column that comes later may have
     * no name yet. An expression may also have no name to look up, as a field of a method result has. Neither is
     * recognized and the condition keeps the default evaluator.
     */
    private static ConditionParameter findColumnParameter(String name, ICondition[] conditions) {
        if (name == null) {
            return null;
        }
        for (ICondition condition : conditions) {
            var params = condition.getParams();
            for (var i = 0; i < params.length; i++) {
                if (params[i] != null && name.equals(params[i].getName())) {
                    return new ConditionParameter(condition, i);
                }
            }
        }
        return null;
    }

    private static Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> twoParameterExpressionParse(
            ICondition condition,
            IBindingContext bindingContext) {
        if (condition.getIndexMethod() != null) {
            var boundNode = condition.getIndexMethod().getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode blockNode) {
                var children = blockNode.getChildren();
                if (children.length == 1 && children[0] instanceof BlockNode node) {
                    blockNode = node;
                    children = blockNode.getChildren();
                    if (children.length == 1 && children[0] instanceof BinaryOpNodeAnd binaryOpNode) {
                        children = binaryOpNode.getChildren();
                        if (children.length == 2 && children[0] instanceof BinaryOpNode binaryOpNode0 && children[1] instanceof BinaryOpNode binaryOpNode1) {
                            var parsedExpr1 = parseBinaryOpExpression(binaryOpNode0,
                                    bindingContext);
                            var parsedExpr2 = parseBinaryOpExpression(binaryOpNode1,
                                    bindingContext);

                            if (parsedExpr1 != null && parsedExpr2 != null) {
                                if (RelationType.EQ.equals(parsedExpr1.getMiddle()) || RelationType.EQ
                                        .equals(parsedExpr2.getMiddle())) {
                                    return null;
                                }
                                return Pair.of(parsedExpr1, parsedExpr2);
                            }
                        }
                    }
                }
            }
            return null;
        }
        throw new IllegalStateException("Condition method is not an instance of CompositeMethod.");
    }

    private static EvaluatorFactory determineOptimizedEvaluationFactory(ICondition condition,
                                                                        IMethodSignature signature,
                                                                        IBindingContext bindingContext,
                                                                        ICondition[] conditions) {
        var params = condition.getParams();

        var code = condition.getIndexSourceCodeModule().getCode();
        if (code == null) {
            return null;
        }

        switch (params.length) {
            case 1:
                var emptyOrContains = emptyOrContainsParse(condition, bindingContext);
                if (emptyOrContains != null) {
                    return makeOneParameterContainsFactory(emptyOrContains, condition, signature, conditions);
                }
                var containsChain = containsChainParse(condition, bindingContext);
                if (containsChain != null) {
                    return makeContainsInInputArrayChainFactory(containsChain, signature, conditions);
                }
                var parsedExpression = oneParameterExpressionParse(condition,
                        bindingContext);
                if (parsedExpression == null) {
                    return null;
                }
                switch (parsedExpression.getMiddle()) {
                    case EQ:
                        return makeOneParameterEqualsFactory(parsedExpression, condition, signature);
                    case IN:
                        return makeOneParameterContainsFactory(parsedExpression, condition, signature, conditions);
                    default:
                        return makeOneParameterRangeFactory(parsedExpression, condition, signature);
                }
            case 2:
                var parsedExpressionWithTwoParams = twoParameterExpressionParse(
                        condition,
                        bindingContext);
                if (parsedExpressionWithTwoParams == null) {
                    return null;
                }
                return makeTwoParameterRangeFactory(parsedExpressionWithTwoParams, condition, signature);
            default:
                return null;
        }

    }

    private static String getOrBuildParameterPath(String p, IParameterDeclaration signatureParam) {
        if (p.startsWith(signatureParam.getName() + "[") || p.startsWith(signatureParam.getName() + ".") || p
                .equals(signatureParam.getName())) {
            return p;
        } else {
            return signatureParam.getName() + "." + p;
        }
    }

    private static EvaluatorFactory makeOneParameterContainsFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature,
            ICondition[] conditions) {
        final var p1 = parsedExpression.getLeft();
        final var p2 = parsedExpression.getRight();

        var conditionParam = condition.getParams()[0];
        IParameterDeclaration signatureParam = getParameter(p1, signature);
        if (signatureParam == null) {
            // contains(columnValues, input): the column keeps an array of values
            signatureParam = getParameter(p2, signature);
            if (signatureParam == null) {
                return null;
            }
            if (!p1.equals(conditionParam.getName())) {
                return null;
            }
            return new OneParameterContainsInFactory(signatureParam, getOrBuildParameterPath(p2, signatureParam));
        }

        // contains(inputs, columnValue): the input keeps an array of values
        if (!p2.equals(conditionParam.getName()) || getParameter(p2, signature) != null || findColumnParameter(p1,
                conditions) != null) {
            return null;
        }
        return new OneParameterContainsInInputArrayFactory(signatureParam, getOrBuildParameterPath(p1, signatureParam));
    }

    private static OneParameterEqualsFactory makeOneParameterEqualsFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature) {
        final var p1 = parsedExpression.getLeft();
        final var p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p1, signature);
        var conditionParam = condition.getParams()[0];

        if (signatureParam == null) {
            signatureParam = getParameter(p2, signature);
            if (signatureParam == null) {
                return null;
            }
            if (!p1.equals(conditionParam.getName())) {
                return null;
            }
            return new OneParameterEqualsFactory(signatureParam, getOrBuildParameterPath(p2, signatureParam));
        }

        if (!p2.equals(conditionParam.getName())) {
            return null;
        }

        return new OneParameterEqualsFactory(signatureParam, getOrBuildParameterPath(p1, signatureParam));
    }

    private static OneParameterRangeFactory makeOneParameterRangeFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature) {
        final var p1 = parsedExpression.getLeft();
        final var p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p1, signature);

        if (signatureParam == null) {
            return makeOppositeOneParameterRangeFactory(parsedExpression, condition, signature);
        }

        var conditionParam = condition.getParams()[0];

        if (!p2.equals(conditionParam.getName())) {
            return null;
        }

        return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                parsedExpression.getMiddle(),
                getOrBuildParameterPath(p1, signatureParam));
    }

    private static TwoParameterRangeFactory makeTwoParameterRangeFactory(
            Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> parsedExpressionWithTwoParams,
            ICondition condition,
            IMethodSignature signature) {
        Triple<String, RelationType, String> expr1 = parsedExpressionWithTwoParams.getLeft();
        if (!expr1.getMiddle().isLessThan()) {
            expr1 = flipOverParsedExpression(expr1);
        }

        Triple<String, RelationType, String> expr2 = parsedExpressionWithTwoParams.getRight();
        if (!expr2.getMiddle().isLessThan()) {
            expr2 = flipOverParsedExpression(expr2);
        }

        if (expr1.getRight().equals(expr2.getLeft())) {
            return makeTwoParameterRangeFactory1(Pair.of(expr1, expr2), condition, signature);
        }

        if (expr1.getLeft().equals(expr2.getRight())) {
            return makeTwoParameterRangeFactory1(Pair.of(expr2, expr1), condition, signature);
        }

        return null;

    }

    private static Triple<String, RelationType, String> flipOverParsedExpression(
            Triple<String, RelationType, String> parsedExpr1) {
        return Triple.of(parsedExpr1.getRight(), parsedExpr1.getMiddle().oposite(), parsedExpr1.getLeft());
    }

    private static TwoParameterRangeFactory makeTwoParameterRangeFactory1(
            Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> parsedExpressionWithTwoParams,
            ICondition condition,
            IMethodSignature signature) {
        Triple<String, RelationType, String> expr1 = parsedExpressionWithTwoParams.getLeft();
        Triple<String, RelationType, String> expr2 = parsedExpressionWithTwoParams.getRight();

        IParameterDeclaration signatureParam = getParameter(expr1.getRight(), signature);

        if (signatureParam == null) {
            return null;
        }

        var conditionParam1 = condition.getParams()[0];

        if (!expr1.getLeft().equals(conditionParam1.getName())) {
            return null;
        }

        var conditionParam2 = condition.getParams()[1];

        if (!expr2.getRight().equals(conditionParam2.getName())) {
            return null;
        }

        return new TwoParameterRangeFactory(signatureParam,
                conditionParam1,
                expr1.getMiddle(),
                conditionParam2,
                expr2.getMiddle(),
                getOrBuildParameterPath(expr1.getRight(), signatureParam));

    }

    private static IParameterDeclaration getParameter(String pname, IMethodSignature signature) {
        if (pname == null) {
            return null;
        }
        var parameterName = pname;
        var dotIndex = parameterName.indexOf('.');
        if (dotIndex > 0) {
            parameterName = parameterName.substring(0, dotIndex);
            var brIndex = parameterName.indexOf('[');
            if (brIndex > 0) {
                parameterName = parameterName.substring(0, brIndex);
            }
        }

        for (var i = 0; i < signature.getNumberOfParameters(); i++) {
            if (parameterName.equals(signature.getParameterName(i))) {
                return new ParameterDeclaration(signature.getParameterType(i), parameterName);
            }
        }

        for (var i = 0; i < signature.getNumberOfParameters(); i++) {
            if (signature.getParameterType(i).getField(parameterName, false) != null) {
                return new ParameterDeclaration(signature.getParameterType(i), signature.getParameterName(i));
            }
        }

        return null;
    }

    private static OneParameterRangeFactory makeOppositeOneParameterRangeFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature) {

        final var p1 = parsedExpression.getLeft();
        final var p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p2, signature);

        if (signatureParam == null) {
            return null;
        }

        var conditionParam = condition.getParams()[0];

        if (!p1.equals(conditionParam.getName())) {
            return null;
        }

        return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                parsedExpression.getMiddle().oposite(),
                getOrBuildParameterPath(p2, signatureParam));
    }

    enum Bound {
        LOWER,
        UPPER
    }

    @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
    enum RelationType {

        LT("<", ">", true, null),
        LE("<=", ">=", true, Bound.UPPER),
        GE(">=", "<=", false, null),
        GT(">", "<", false, Bound.LOWER),
        EQ("==", "==", false, null),
        IN("in", "in", false, null);

        final String func;
        final String opposite;
        @Getter
        final boolean lessThan;
        @Getter
        final Bound incBound;

        public RelationType oposite() {
            switch (this) {
                case LT:
                    return GT;
                case GT:
                    return LT;
                case LE:
                    return GE;
                case GE:
                    return LE;
                case EQ:
                    return EQ;
                default:
                    throw new IllegalStateException();
            }
        }
    }

    static class RelationRangeAdaptor<C extends Comparable<C>> implements IRangeAdaptor<Object, C> {
        final EvaluatorFactory evaluatorFactory;
        final ITypeAdaptor<Object, C> typeAdaptor;
        final ConditionCasts conditionCasts;

        @SuppressWarnings("unchecked")
        RelationRangeAdaptor(EvaluatorFactory evaluatorFactory,
                             ITypeAdaptor<?, C> typeAdaptor,
                             ConditionCasts conditionCasts) {
            super();
            this.evaluatorFactory = evaluatorFactory;
            this.typeAdaptor = (ITypeAdaptor<Object, C>) typeAdaptor;
            this.conditionCasts = Objects.requireNonNull(conditionCasts, "conditionsCasts cannot be null");
        }

        @Override
        public C getMax(Object param) {
            if (param == null) {
                return null;
            }
            if (evaluatorFactory.hasMax()) {
                param = conditionCasts.castToInputType(param);
                var v = typeAdaptor.convert(param);
                if (evaluatorFactory.needsIncrement(Bound.UPPER)) {
                    v = typeAdaptor.increment(v);
                }
                return v;
            }

            return null;
        }

        @Override
        public C getMin(Object param) {
            if (param == null) {
                return null;
            }
            if (evaluatorFactory.hasMin()) {
                param = conditionCasts.castToInputType(param);
                var v = typeAdaptor.convert(param);
                if (evaluatorFactory.needsIncrement(Bound.LOWER)) {
                    v = typeAdaptor.increment(v);
                }
                return v;
            }

            return null;
        }

        @Override
        public C adaptValueType(Object value) {
            value = conditionCasts.castToConditionType(value);
            return typeAdaptor.convert(value);
        }

        @Override
        public boolean useOriginalSource() {
            return true;
        }

    }

    public static class OneParameterContainsInArrayIndexedEvaluator extends ContainsInArrayIndexedEvaluator {
        private final OneParameterContainsInFactory oneParameterContainsInFactory;

        OneParameterContainsInArrayIndexedEvaluator(OneParameterContainsInFactory oneParameterContainsInFactory,
                                                           ConditionCasts conditionCasts) {
            super(conditionCasts);
            this.oneParameterContainsInFactory = Objects.requireNonNull(oneParameterContainsInFactory,
                    "oneParameterContainsInFactory cannot be null");
        }

        @Override
        public String getOptimizedSourceCode() {
            return oneParameterContainsInFactory.getExpression();
        }

        @Override
        public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
            return condition instanceof ICondition ? ((Condition) condition).getIndexSourceCodeModule() : condition.getSourceCodeModule();
        }
    }

    public static class OneParameterContainsInArrayIndexedEvaluatorV2 extends ContainsInArrayIndexedEvaluatorV2 {
        private final OneParameterContainsInFactory oneParameterContainsInFactory;

        OneParameterContainsInArrayIndexedEvaluatorV2(OneParameterContainsInFactory oneParameterContainsInFactory,
                                                             ConditionCasts conditionCasts) {
            super(conditionCasts);
            this.oneParameterContainsInFactory = Objects.requireNonNull(oneParameterContainsInFactory,
                    "oneParameterContainsInFactory cannot be null");
        }

        @Override
        public String getOptimizedSourceCode() {
            return oneParameterContainsInFactory.getExpression();
        }

        @Override
        public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
            return condition instanceof ICondition ? ((Condition) condition).getIndexSourceCodeModule() : condition.getSourceCodeModule();
        }
    }

    public static class OneParameterEqualsIndexedEvaluator extends EqualsIndexedEvaluator {
        private final OneParameterEqualsFactory oneParameterEqualsFactory;

        OneParameterEqualsIndexedEvaluator(OneParameterEqualsFactory oneParameterEqualsFactory,
                                                  ConditionCasts conditionCasts) {
            super(conditionCasts);
            this.oneParameterEqualsFactory = Objects.requireNonNull(oneParameterEqualsFactory,
                    "oneParameterEqualsFactory cannot be null");
        }

        @Override
        public String getOptimizedSourceCode() {
            return oneParameterEqualsFactory.getExpression();
        }

        @Override
        public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
            return condition instanceof ICondition ? ((Condition) condition).getIndexSourceCodeModule() : condition.getSourceCodeModule();
        }
    }

    public static class OneParameterEqualsIndexedEvaluatorV2 extends EqualsIndexedEvaluatorV2 {
        private final OneParameterEqualsFactory oneParameterEqualsFactory;

        OneParameterEqualsIndexedEvaluatorV2(OneParameterEqualsFactory oneParameterEqualsFactory,
                                                    ConditionCasts conditionCasts) {
            super(conditionCasts);
            this.oneParameterEqualsFactory = Objects.requireNonNull(oneParameterEqualsFactory,
                    "oneParameterEqualsFactory cannot be null");
        }

        @Override
        public String getOptimizedSourceCode() {
            return oneParameterEqualsFactory.getExpression();
        }

        @Override
        public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
            return condition instanceof ICondition ? ((Condition) condition).getIndexSourceCodeModule() : condition.getSourceCodeModule();
        }
    }

    abstract static class EvaluatorFactory {

        final IParameterDeclaration signatureParam;
        @Getter
        final String expression;

        EvaluatorFactory(IParameterDeclaration signatureParam, String expression) {
            super();
            this.signatureParam = signatureParam;
            this.expression = expression;
        }

        public abstract boolean hasMin();

        public abstract boolean hasMax();

        public abstract boolean needsIncrement(Bound bound);

        IOpenClass getExpressionType() {
            return DecisionTableAlgorithmBuilder.findExpressionType(signatureParam.getType(), expression);
        }

    }

    static class OneParameterContainsInFactory extends EvaluatorFactory {

        public OneParameterContainsInFactory(IParameterDeclaration signatureParam, String expression) {
            super(signatureParam, expression);
        }

        @Override
        public boolean hasMin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMax() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean needsIncrement(Bound bound) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Describes a chain of {@code contains(inputArray, columnValue)} calls joined by {@code or}. The expression is
     * the path to the array passed to the decision table, and the values are the column parameters looked up in it.
     */
    static class ContainsInInputArrayChainFactory extends OneParameterContainsInInputArrayFactory {

        @Getter
        private final List<ConditionParameter> values;

        public ContainsInInputArrayChainFactory(IParameterDeclaration signatureParam,
                                                String expression,
                                                List<ConditionParameter> values) {
            super(signatureParam, expression);
            this.values = List.copyOf(values);
        }
    }

    /**
     * Describes a {@code contains(inputArray, columnValue)} condition. The expression is the path to the array
     * passed to the decision table.
     */
    static class OneParameterContainsInInputArrayFactory extends EvaluatorFactory {

        public OneParameterContainsInInputArrayFactory(IParameterDeclaration signatureParam, String expression) {
            super(signatureParam, expression);
        }

        @Override
        public boolean hasMin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMax() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean needsIncrement(Bound bound) {
            throw new UnsupportedOperationException();
        }
    }

    static class OneParameterEqualsFactory extends EvaluatorFactory {
        public OneParameterEqualsFactory(IParameterDeclaration signatureParam, String expression) {
            super(signatureParam, expression);
        }

        @Override
        public boolean hasMin() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasMax() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean needsIncrement(Bound bound) {
            throw new UnsupportedOperationException();
        }

    }

    static class OneParameterRangeFactory extends EvaluatorFactory {
        final IParameterDeclaration conditionParam;
        final RelationType relation;

        public OneParameterRangeFactory(IParameterDeclaration signatureParam,
                                        IParameterDeclaration conditionParam,
                                        RelationType relation,
                                        String expression) {
            super(signatureParam, expression);

            this.conditionParam = conditionParam;
            this.relation = relation;
        }

        @Override
        public boolean hasMin() {
            return !relation.isLessThan();
        }

        @Override
        public boolean hasMax() {
            return relation.isLessThan();
        }

        @Override
        public boolean needsIncrement(Bound bound) {
            return relation.getIncBound() == bound;
        }

    }

    static class TwoParameterRangeFactory extends EvaluatorFactory {
        final IParameterDeclaration conditionParam1;
        final IParameterDeclaration conditionParam2;
        final RelationType relation1;
        final RelationType relation2;

        public TwoParameterRangeFactory(IParameterDeclaration signatureParam,
                                        IParameterDeclaration conditionParam1,
                                        RelationType relation1,
                                        IParameterDeclaration conditionParam2,
                                        RelationType relation2,
                                        String expression) {
            super(signatureParam, expression);

            this.conditionParam1 = conditionParam1;
            this.relation1 = relation1;
            this.conditionParam2 = conditionParam2;
            this.relation2 = relation2;
        }

        @Override
        public boolean hasMin() {
            return true;
        }

        @Override
        public boolean hasMax() {
            return true;
        }

        @Override
        public boolean needsIncrement(Bound bound) {
            if (bound == Bound.LOWER) {
                return relation1 == RelationType.LT;
            }
            return relation2 == RelationType.LE;
        }

    }

}
