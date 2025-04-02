package org.openl.rules.dt.algorithm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Objects;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BinaryOpNode;
import org.openl.binding.impl.BinaryOpNodeAnd;
import org.openl.binding.impl.BindHelper;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.binding.impl.IndexNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.binding.impl.MethodBoundNode;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.AConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.CombinedRangeIndexEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluatorV2;
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
import org.openl.rules.range.Range;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.NaryNode;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.ParameterDeclaration;

class DependentParametersOptimizedAlgorithm {

    static IConditionEvaluator makeEvaluator(ICondition condition,
                                             IMethodSignature signature,
                                             IBindingContext bindingContext) {
        if (condition.hasFormulas() || condition.isRuleIdOrRuleNameUsed()) {
            return null;
        }

        EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(condition, signature, bindingContext);

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
        IOpenClass expressionType = evaluatorFactory.getExpressionType();
        if (expressionType == null) {
            // Fall back to default evaluator
            return null;
        }
        IParameterDeclaration[] params = condition.getParams();
        IOpenClass conditionParamType0 = params[0].getType();
        IOpenClass conditionParamType1 = params[1].getType();

        if (conditionParamType0.equals(conditionParamType1)) {
            ConditionCasts conditionCasts = ConditionHelper
                    .findConditionCasts(conditionParamType0, expressionType, bindingContext);

            if (!conditionCasts.atLeastOneExists()) {
                String message = String.format(
                        "Cannot convert from '%s' to '%s'. Incompatible types comparison in '%s' condition.",
                        conditionParamType0.getName(),
                        expressionType.getName(),
                        condition.getName());
                BindHelper.processError(message, condition.getUserDefinedExpressionSource(), bindingContext);
                return null;
            }

            IRangeAdaptor<?, ? extends Comparable<?>> adaptor = getRangeAdaptor(evaluatorFactory,
                    conditionParamType0,
                    expressionType,
                    conditionCasts);

            if (adaptor == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            CombinedRangeIndexEvaluator rix = new CombinedRangeIndexEvaluator(
                    (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor,
                    2,
                    ConditionHelper.getConditionCastsWithNoCasts());

            rix.setOptimizedSourceCode(evaluatorFactory.getExpression());

            return rix;
        }
        return null;
    }

    /**
     * Creates an optimized evaluator for one-parameter conditions.
     *
     * <p>This method selects and constructs an appropriate evaluator based on the condition's parameter type,
     * the expected expression type from the evaluator factory, and the necessary type casts. For "contains"
     * evaluations (when the factory is an instance of OneParameterContainsInFactory), it handles aggregate types
     * by verifying the component type and ensuring range arrays are not processed. For "equals" or range-based
     * strategies, the method checks type compatibility and retrieves a suitable range adaptor if needed. In cases
     * where required casts are unavailable or the configuration is unsupported, the method logs an error via the
     * binding context and returns null.</p>
     *
     * @param condition the condition to be evaluated
     * @param bindingContext context used for type conversion and error reporting
     * @param evaluatorFactory the factory that provides the evaluation strategy and target expression type
     * @return an optimized evaluator for the condition, or null if a valid evaluator cannot be created
     */
    private static IConditionEvaluator makeOneParamEvaluator(ICondition condition,
                                                             IBindingContext bindingContext,
                                                             EvaluatorFactory evaluatorFactory) {
        IOpenClass expressionType = evaluatorFactory.getExpressionType();
        if (expressionType == null) {
            // Fall back to default evaluator
            return null;
        }
        IParameterDeclaration[] params = condition.getParams();
        IOpenClass conditionParamType = params[0].getType();

        if (evaluatorFactory instanceof OneParameterContainsInFactory) {
            IAggregateInfo aggregateInfo = conditionParamType.getAggregateInfo();
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
                            (OneParameterContainsInFactory) evaluatorFactory,
                            aggregateConditionCasts)
                            : new OneParameterContainsInArrayIndexedEvaluator(
                            (OneParameterContainsInFactory) evaluatorFactory,
                            aggregateConditionCasts);
                }
            }
            return null;
        }

        ConditionCasts conditionCasts = ConditionHelper
                .findConditionCasts(conditionParamType, expressionType, bindingContext);

        if (!conditionCasts.atLeastOneExists()) {
            String message = String.format(
                    "Cannot convert from '%s' to '%s'. Incompatible types comparison in '%s' condition.",
                    conditionParamType.getName(),
                    expressionType.getName(),
                    condition.getName());

            BindHelper.processError(message, condition.getUserDefinedExpressionSource(), bindingContext);
            return null;
        }

        if (evaluatorFactory instanceof OneParameterEqualsFactory) {
            if (!conditionParamType.isArray() && !expressionType.isArray()) {
                return condition.getNumberOfEmptyRules(0) > 1 || condition.getStaticMethod() != null
                        ? new OneParameterEqualsIndexedEvaluatorV2(
                        (OneParameterEqualsFactory) evaluatorFactory,
                        conditionCasts)
                        : new OneParameterEqualsIndexedEvaluator(
                        (OneParameterEqualsFactory) evaluatorFactory,
                        conditionCasts);
            }
        } else {
            IRangeAdaptor<?, ? extends Comparable<?>> adaptor = getRangeAdaptor(evaluatorFactory,
                    conditionParamType,
                    expressionType,
                    conditionCasts);

            if (adaptor == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            AConditionEvaluator rix = new SingleRangeIndexEvaluator(
                    (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor,
                    conditionCasts);
            rix.setOptimizedSourceCode(evaluatorFactory.getExpression());
            return rix;
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
        IBoundNode[] children = indexNode.getChildren();
        if (children != null && children.length == 1 && children[0] instanceof LiteralBoundNode) {
            LiteralBoundNode literalBoundNode = (LiteralBoundNode) children[0];
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
        String value = field.getFieldName();
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
        if (children != null && children.length == 2 && children[0] instanceof FieldBoundNode && children[1] instanceof FieldBoundNode) {
            RelationType relationType;
            if (isContainsMethod(methodBoundNode)) {
                relationType = RelationType.IN;
            } else {
                return null;
            }
            FieldBoundNode fieldBoundNode0 = (FieldBoundNode) children[0];
            FieldBoundNode fieldBoundNode1 = (FieldBoundNode) children[1];
            return Triple.of(buildFieldName(fieldBoundNode0, ctx), relationType, buildFieldName(fieldBoundNode1, ctx));
        }

        return null;
    }

    private static boolean isContainsMethod(MethodBoundNode methodBoundNode) {
        if (methodBoundNode.getSyntaxNode() instanceof NaryNode) {
            var children = ((NaryNode) methodBoundNode.getSyntaxNode()).getNodes();
            if (children.length == 3) {
                var identifier = children[2];
                return "funcname".equals(identifier.getType()) && "contains".equals(((IdentifierNode) identifier).getIdentifier());
            }
        }
        return false;
    }

    private static Triple<String, RelationType, String> parseBinaryOpExpression(BinaryOpNode binaryOpNode,
                                                                                IBindingContext bindingContext) {
        IBoundNode[] children = binaryOpNode.getChildren();
        if (children != null && children.length == 2 && children[0] instanceof FieldBoundNode && children[1] instanceof FieldBoundNode) {
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
            FieldBoundNode fieldBoundNode0 = (FieldBoundNode) children[0];
            FieldBoundNode fieldBoundNode1 = (FieldBoundNode) children[1];

            return Triple.of(buildFieldName(fieldBoundNode0, bindingContext),
                    relationType,
                    buildFieldName(fieldBoundNode1, bindingContext));
        }
        return null;
    }

    /**
     * Parses the one-parameter expression from the specified condition.
     *
     * <p>This method retrieves the bound node from the condition's index method and inspects its structure. If the node is a block
     * containing a single nested block with exactly one child that is either a binary operation or a method-bound expression,
     * the corresponding parsing method is invoked to extract the expression components. These components are returned as a Triple, 
     * where the first element is typically a field or parameter name, the second is a relation type, and the third is an expression 
     * or literal value.
     *
     * @param condition      the condition containing the expression to parse
     * @param bindingContext the binding context used during parsing
     * @return a Triple with the parsed expression components, or null if the node structure does not match the expected format
     * @throws IllegalStateException if the condition does not provide a valid index method representing a composite method
     */
    private static Triple<String, RelationType, String> oneParameterExpressionParse(ICondition condition,
                                                                                    IBindingContext bindingContext) {
        if (condition.getIndexMethod() != null) {
            IBoundNode boundNode = condition.getIndexMethod().getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode) {
                BlockNode blockNode = (BlockNode) boundNode;
                IBoundNode[] children = blockNode.getChildren();
                if (children != null && children.length == 1 && children[0] instanceof BlockNode) {
                    blockNode = (BlockNode) children[0];
                    children = blockNode.getChildren();
                    if (children.length == 1) {
                        if (children[0] instanceof BinaryOpNode) {
                            BinaryOpNode binaryOpNode = (BinaryOpNode) children[0];
                            return parseBinaryOpExpression(binaryOpNode, bindingContext);
                        } else if (children[0] instanceof MethodBoundNode) {
                            var methodBoundNode = (MethodBoundNode) children[0];
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
     * Parses a two-parameter indexed expression from the given condition into a pair of binary relational expressions.
     * 
     * <p>This method processes the bound node derived from the condition's index method to extract two binary expressions.
     * Each expression is parsed into a triple containing the left operand, the relational operator, and the right operand.
     * The method returns {@code null} if either expression uses an equality operator or if the expected structure is not met.
     * An {@link IllegalStateException} is thrown if the condition lacks a valid index method.</p>
     *
     * @param condition the condition whose indexed expression is to be parsed
     * @param bindingContext the context used during parsing
     * @return a pair of triples representing the parsed binary expressions, or {@code null} if parsing fails or an equality relation is found
     * @throws IllegalStateException if the condition does not have an index method
     */
    private static Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> twoParameterExpressionParse(
            ICondition condition,
            IBindingContext bindingContext) {
        if (condition.getIndexMethod() != null) {
            IBoundNode boundNode = condition.getIndexMethod().getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode) {
                BlockNode blockNode = (BlockNode) boundNode;
                IBoundNode[] children = blockNode.getChildren();
                if (children.length == 1 && children[0] instanceof BlockNode) {
                    blockNode = (BlockNode) children[0];
                    children = blockNode.getChildren();
                    if (children.length == 1 && children[0] instanceof BinaryOpNodeAnd) {
                        BinaryOpNodeAnd binaryOpNode = (BinaryOpNodeAnd) children[0];
                        children = binaryOpNode.getChildren();
                        if (children.length == 2 && children[0] instanceof BinaryOpNode && children[1] instanceof BinaryOpNode) {
                            BinaryOpNode binaryOpNode0 = (BinaryOpNode) children[0];
                            BinaryOpNode binaryOpNode1 = (BinaryOpNode) children[1];
                            Triple<String, RelationType, String> parsedExpr1 = parseBinaryOpExpression(binaryOpNode0,
                                    bindingContext);
                            Triple<String, RelationType, String> parsedExpr2 = parseBinaryOpExpression(binaryOpNode1,
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

    /**
     * Determines the optimized evaluator factory for a given condition by parsing its index source code and parameter expressions.
     *
     * <p>This method first checks for the presence of index source code. For a single-parameter condition, it parses the expression
     * and selects an evaluator factory based on the relation type (equality, containment, or range). For a two-parameter condition,
     * it parses a pair of binary expressions to construct a two-parameter range evaluator factory. If the index source code is missing or
     * the expression(s) cannot be parsed, this method returns {@code null}.
     *
     * @param condition the condition to be evaluated
     * @param signature the method signature associated with the condition
     * @param bindingContext the context providing necessary binding and type information
     * @return an {@code EvaluatorFactory} instance tailored to the condition, or {@code null} if no suitable factory can be determined
     */
    private static EvaluatorFactory determineOptimizedEvaluationFactory(ICondition condition,
                                                                        IMethodSignature signature,
                                                                        IBindingContext bindingContext) {
        IParameterDeclaration[] params = condition.getParams();

        String code = condition.getIndexSourceCodeModule().getCode();
        if (code == null) {
            return null;
        }

        switch (params.length) {
            case 1:
                Triple<String, RelationType, String> parsedExpression = oneParameterExpressionParse(condition,
                        bindingContext);
                if (parsedExpression == null) {
                    return null;
                }
                switch (parsedExpression.getMiddle()) {
                    case EQ:
                        return makeOneParameterEqualsFactory(parsedExpression, condition, signature);
                    case IN:
                        return makeOneParameterContainsFactory(parsedExpression, condition, signature);
                    default:
                        return makeOneParameterRangeFactory(parsedExpression, condition, signature);
                }
            case 2:
                Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> parsedExpressionWithTwoParams = twoParameterExpressionParse(
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

    private static OneParameterContainsInFactory makeOneParameterContainsFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature) {
        final String p1 = parsedExpression.getLeft();
        final String p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p1, signature);
        if (signatureParam == null) {
            signatureParam = getParameter(p2, signature);
            if (signatureParam == null) {
                return null;
            }
            IParameterDeclaration conditionParam = condition.getParams()[0];
            if (!p1.equals(conditionParam.getName())) {
                return null;
            }
            return new OneParameterContainsInFactory(signatureParam, getOrBuildParameterPath(p2, signatureParam));
        }

        return null;
    }

    private static OneParameterEqualsFactory makeOneParameterEqualsFactory(
            Triple<String, RelationType, String> parsedExpression,
            ICondition condition,
            IMethodSignature signature) {
        final String p1 = parsedExpression.getLeft();
        final String p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p1, signature);
        IParameterDeclaration conditionParam = condition.getParams()[0];

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
        final String p1 = parsedExpression.getLeft();
        final String p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p1, signature);

        if (signatureParam == null) {
            return makeOppositeOneParameterRangeFactory(parsedExpression, condition, signature);
        }

        IParameterDeclaration conditionParam = condition.getParams()[0];

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

        IParameterDeclaration conditionParam1 = condition.getParams()[0];

        if (!expr1.getLeft().equals(conditionParam1.getName())) {
            return null;
        }

        IParameterDeclaration conditionParam2 = condition.getParams()[1];

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
        String parameterName = pname;
        int dotIndex = parameterName.indexOf('.');
        if (dotIndex > 0) {
            parameterName = parameterName.substring(0, dotIndex);
            int brIndex = parameterName.indexOf('[');
            if (brIndex > 0) {
                parameterName = parameterName.substring(0, brIndex);
            }
        }

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            if (parameterName.equals(signature.getParameterName(i))) {
                return new ParameterDeclaration(signature.getParameterType(i), parameterName);
            }
        }

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
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

        final String p1 = parsedExpression.getLeft();
        final String p2 = parsedExpression.getRight();

        IParameterDeclaration signatureParam = getParameter(p2, signature);

        if (signatureParam == null) {
            return null;
        }

        IParameterDeclaration conditionParam = condition.getParams()[0];

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

    enum RelationType {

        LT("<", ">", true, null),
        LE("<=", ">=", true, Bound.UPPER),
        GE(">=", "<=", false, null),
        GT(">", "<", false, Bound.LOWER),
        EQ("==", "==", false, null),
        IN("in", "in", false, null);

        final String func;
        final String opposite;
        final boolean lessThan;
        final Bound incBound;

        RelationType(String func, String opposite, boolean lessThan, Bound incBound) {
            this.func = func;
            this.opposite = opposite;
            this.lessThan = lessThan;
            this.incBound = incBound;
        }

        public Bound getIncBound() {
            return incBound;
        }

        public boolean isLessThan() {
            return lessThan;
        }

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
                C v = typeAdaptor.convert(param);
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
                C v = typeAdaptor.convert(param);
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

        /**
         * Returns the formal source code module for the specified condition.
         * <p>
         * If the provided condition is an instance of {@code ICondition}, the index source code module is returned;
         * otherwise, the default source code module is provided.
         * </p>
         *
         * @param condition the base condition from which to extract the source code module
         * @return the formal source code module associated with the condition
         */
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

        /**
         * Retrieves the formal source code module for the specified condition.
         *
         * <p>If the condition is an instance of ICondition, its associated index source code module is returned;
         * otherwise, the condition's default source code module is returned.
         *
         * @param condition the condition from which to extract the source code module
         * @return the appropriate formal source code module for the given condition
         */
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

        /**
         * Retrieves the formal source code module associated with the given condition.
         * <p>
         * If the condition is an instance of {@code ICondition}, the index-specific source code module is returned;
         * otherwise, the default source code module of the condition is used.
         * </p>
         *
         * @param condition the condition from which to extract the source code module
         * @return the formal source code module for the condition
         */
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

        /**
         * Returns the formal source code module for the specified condition.
         * <p>
         * If the condition is an instance of ICondition, its index source code module is returned;
         * otherwise, the default source code module is used.
         * </p>
         *
         * @param condition the condition to retrieve the source code module for
         * @return the formal source code module corresponding to the condition
         */
        @Override
        public IOpenSourceCodeModule getFormalSourceCode(IBaseCondition condition) {
            return condition instanceof ICondition ? ((Condition) condition).getIndexSourceCodeModule() : condition.getSourceCodeModule();
        }
    }

    abstract static class EvaluatorFactory {

        final IParameterDeclaration signatureParam;
        final String expression;

        EvaluatorFactory(IParameterDeclaration signatureParam, String expression) {
            super();
            this.signatureParam = signatureParam;
            this.expression = expression;
        }

        public abstract boolean hasMin();

        public abstract boolean hasMax();

        public abstract boolean needsIncrement(Bound bound);

        public String getExpression() {
            return expression;
        }

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
