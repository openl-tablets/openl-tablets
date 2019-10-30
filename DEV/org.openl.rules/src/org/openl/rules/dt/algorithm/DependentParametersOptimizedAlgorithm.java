package org.openl.rules.dt.algorithm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.*;
import org.openl.meta.*;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.*;
import org.openl.rules.dt.element.ConditionCasts;
import org.openl.rules.dt.element.ConditionHelper;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.ITypeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;

class DependentParametersOptimizedAlgorithm {

    RangeEvaluatorFactory[] rangeFactories = { new RangeEvaluatorFactory(null, 0, 0, 0) };

    static IConditionEvaluator makeEvaluator(ICondition condition,
            IMethodSignature signature,
            IBindingContext bindingContext) throws SyntaxNodeException {
        if (condition.hasFormulas()) {
            return null;
        }

        EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(condition, signature);

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
            EvaluatorFactory evaluatorFactory) throws SyntaxNodeException {
        IOpenClass expressionType = evaluatorFactory.getExpressionType();
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
                throw new SyntaxNodeException(message, null, null, condition.getUserDefinedExpressionSource());
            }

            IRangeAdaptor<?, ? extends Comparable<?>> adaptor = getRangeAdaptor(evaluatorFactory,
                conditionParamType0,
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

    private static IConditionEvaluator makeOneParamEvaluator(ICondition condition,
            IBindingContext bindingContext,
            EvaluatorFactory evaluatorFactory) throws SyntaxNodeException {
        IOpenClass expressionType = evaluatorFactory.getExpressionType();
        IParameterDeclaration[] params = condition.getParams();
        IOpenClass conditionParamType = params[0].getType();

        ConditionCasts conditionCasts = ConditionHelper
            .findConditionCasts(conditionParamType, expressionType, bindingContext);

        if (!conditionCasts.atLeastOneExists()) {
            String message = String.format(
                "Cannot convert from '%s' to '%s'. Incompatible types comparison in '%s' condition.",
                conditionParamType.getName(),
                expressionType.getName(),
                condition.getName());

            throw new SyntaxNodeException(message, null, null, condition.getUserDefinedExpressionSource());
        }

        if (evaluatorFactory instanceof OneParameterEqualsFactory) {
            if (!conditionParamType.isArray() && !expressionType.isArray()) {
                return condition.getNumberOfEmptyRules(0) > 1
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
            ConditionCasts conditionCasts) {

        Class<?> typeClass = paramType.getInstanceClass();
        if (typeClass.equals(String.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.STRING, conditionCasts);
        }

        if (typeClass.equals(StringValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.STRING_VALUE, conditionCasts);
        }

        if (typeClass.equals(byte.class) || typeClass.equals(Byte.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BYTE, conditionCasts);
        }

        if (typeClass.equals(ByteValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BYTE_VALUE, conditionCasts);
        }

        if (typeClass.equals(short.class) || typeClass.equals(Short.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.SHORT, conditionCasts);
        }

        if (typeClass.equals(ShortValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.SHORT_VALUE, conditionCasts);
        }

        if (typeClass.equals(int.class) || typeClass.equals(Integer.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.INT, conditionCasts);
        }

        if (typeClass.equals(IntValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.INT_VALUE, conditionCasts);
        }

        if (typeClass.equals(long.class) || typeClass.equals(Long.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.LONG, conditionCasts);
        }

        if (typeClass.equals(LongValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.LONG_VALUE, conditionCasts);
        }

        if (typeClass.equals(float.class) || typeClass.equals(Float.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.FLOAT, conditionCasts);
        }

        if (typeClass.equals(FloatValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.FLOAT_VALUE, conditionCasts);
        }

        if (typeClass.equals(double.class) || typeClass.equals(Double.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DOUBLE, conditionCasts);
        }

        if (typeClass.equals(DoubleValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DOUBLE_VALUE, conditionCasts);
        }

        if (typeClass.equals(BigInteger.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGINTEGER, conditionCasts);
        }

        if (typeClass.equals(BigIntegerValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGINTEGER_VALUE, conditionCasts);
        }

        if (typeClass.equals(BigDecimal.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGDECIMAL, conditionCasts);
        }

        if (typeClass.equals(BigDecimalValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGDECIMAL_VALUE, conditionCasts);
        }

        if (typeClass.equals(Date.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DATE, conditionCasts);
        }

        return null;
    }

    private static String buildFieldName(IndexNode indexNode) throws SyntaxNodeException {
        String value = "[";
        IBoundNode[] children = indexNode.getChildren();
        if (children != null && children.length == 1 && children[0] instanceof LiteralBoundNode) {
            LiteralBoundNode literalBoundNode = (LiteralBoundNode) children[0];
            if ("literal.string".equals(literalBoundNode.getSyntaxNode().getType())) {
                value = value + "\"" + literalBoundNode.getValue().toString() + "\"]";
            } else {
                value = value + literalBoundNode.getValue().toString() + "]";
            }
        } else {
            throw new SyntaxNodeException("Cannot parse array index.", null, indexNode.getSyntaxNode());
        }

        if (indexNode.getTargetNode() != null) {
            if (indexNode.getTargetNode() instanceof FieldBoundNode) {
                return buildFieldName((FieldBoundNode) indexNode.getTargetNode()) + value;
            }
            if (indexNode.getTargetNode() instanceof IndexNode) {
                return value + buildFieldName((IndexNode) indexNode.getTargetNode());
            }
            throw new SyntaxNodeException("Cannot parse array index.", null, indexNode.getSyntaxNode());
        }
        return value;
    }

    private static String buildFieldName(FieldBoundNode field) throws SyntaxNodeException {
        String value = field.getFieldName();
        if (field.getTargetNode() != null) {
            if (field.getTargetNode() instanceof FieldBoundNode) {
                return buildFieldName((FieldBoundNode) field.getTargetNode()) + "." + value;
            }
            if (field.getTargetNode() instanceof IndexNode) {
                return buildFieldName((IndexNode) field.getTargetNode()) + "." + value;
            }
            throw new SyntaxNodeException("Cannot parse field name.", null, field.getSyntaxNode());
        }
        return value;
    }

    private static Triple<String, RelationType, String> parseBinaryOpExpression(
            BinaryOpNode binaryOpNode) throws SyntaxNodeException {
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

            return Triple.of(buildFieldName(fieldBoundNode0), relationType, buildFieldName(fieldBoundNode1));
        }
        return null;
    }

    private static Triple<String, RelationType, String> oneParameterExpressionParse(
            ICondition condition) throws SyntaxNodeException {
        if (condition.getMethod() instanceof CompositeMethod) {
            IBoundNode boundNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode) {
                BlockNode blockNode = (BlockNode) boundNode;
                IBoundNode[] children = blockNode.getChildren();
                if (children != null && children.length == 1 && children[0] instanceof BlockNode) {
                    blockNode = (BlockNode) children[0];
                    children = blockNode.getChildren();
                    if (children.length == 1 && children[0] instanceof BinaryOpNode) {
                        BinaryOpNode binaryOpNode = (BinaryOpNode) children[0];
                        return parseBinaryOpExpression(binaryOpNode);
                    }
                }
            }
            return null;
        }
        throw new IllegalStateException("Condition method is not an instance of CompositeMethod.");
    }

    private static Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> twoParameterExpressionParse(
            ICondition condition) throws SyntaxNodeException {
        if (condition.getMethod() instanceof CompositeMethod) {
            IBoundNode boundNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
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
                            Triple<String, RelationType, String> parsedExpr1 = parseBinaryOpExpression(binaryOpNode0);
                            Triple<String, RelationType, String> parsedExpr2 = parseBinaryOpExpression(binaryOpNode1);

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
            IMethodSignature signature) throws SyntaxNodeException {
        IParameterDeclaration[] params = condition.getParams();

        String code = condition.getSourceCodeModule().getCode();
        if (code == null) {
            return null;
        }

        switch (params.length) {
            case 1:
                Triple<String, RelationType, String> parsedExpression = oneParameterExpressionParse(condition);
                if (parsedExpression == null) {
                    return null;
                }
                if (RelationType.EQ.equals(parsedExpression.getMiddle())) {
                    return makeOneParameterEqualsFactory(parsedExpression, condition, signature);
                } else {
                    return makeOneParameterRangeFactory(parsedExpression, condition, signature);
                }
            case 2:
                Pair<Triple<String, RelationType, String>, Triple<String, RelationType, String>> parsedExpressionWithTwoParams = twoParameterExpressionParse(
                    condition);
                if (parsedExpressionWithTwoParams == null) {
                    return null;
                }
                return makeTwoParameterRangeFactory(parsedExpressionWithTwoParams, condition, signature);
            default:
                return null;
        }

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
            if (p2.startsWith(signatureParam.getName() + "[") || p2.startsWith(signatureParam.getName() + ".") || p2
                .equals(signatureParam.getName())) {
                return new OneParameterEqualsFactory(signatureParam, p2);
            } else {
                return new OneParameterEqualsFactory(signatureParam, signatureParam.getName() + "." + p2);
            }
        }

        if (!p2.equals(conditionParam.getName())) {
            return null;
        }

        if (p1.startsWith(signatureParam.getName() + "[") || p1.startsWith(signatureParam.getName() + ".") || p1
            .equals(signatureParam.getName())) {
            return new OneParameterEqualsFactory(signatureParam, p1);
        } else {
            return new OneParameterEqualsFactory(signatureParam, signatureParam.getName() + "." + p1);
        }
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

        if (p1.startsWith(signatureParam.getName() + "[") || p1.startsWith(signatureParam.getName() + ".") || p1
            .equals(signatureParam.getName())) {
            return new OneParameterRangeFactory(signatureParam, conditionParam, parsedExpression.getMiddle(), p1);
        } else {
            return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                parsedExpression.getMiddle(),
                signatureParam.getName() + "." + p1);
        }
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

        final String v = expr1.getRight();

        if (v.startsWith(signatureParam.getName() + "[") || v.startsWith(signatureParam.getName() + ".") || v
            .equals(signatureParam.getName())) {
            return new TwoParameterRangeFactory(signatureParam,
                conditionParam1,
                expr1.getMiddle(),
                conditionParam2,
                expr2.getMiddle(),
                expr1.getRight());
        } else {
            return new TwoParameterRangeFactory(signatureParam,
                conditionParam1,
                expr1.getMiddle(),
                conditionParam2,
                expr2.getMiddle(),
                signatureParam.getName() + "." + v);
        }

    }

    private static IParameterDeclaration getParameter(String pname, IMethodSignature signature) {
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

        if (p2.startsWith(signatureParam.getName() + "[") || p2.startsWith(signatureParam.getName() + ".") || p2
            .equals(signatureParam.getName())) {
            return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                parsedExpression.getMiddle().oposite(),
                p2);
        } else {
            return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                parsedExpression.getMiddle().oposite(),
                signatureParam.getName() + "." + p2);
        }
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
        EQ("==", "==", false, null);

        String func;
        String opposite;
        boolean lessThan;
        Bound incBound;

        private RelationType(String func, String opposite, boolean lessThan, Bound incBound) {
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
        EvaluatorFactory evaluatorFactory;
        ITypeAdaptor<Object, C> typeAdaptor;
        ConditionCasts conditionCasts;

        @SuppressWarnings("unchecked")
        RelationRangeAdaptor(EvaluatorFactory evaluatorFactory,
                ITypeAdaptor<? extends Object, C> typeAdaptor,
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

    static class RangeEvaluatorFactory {

        Pattern pattern;
        String regex;
        int numberOfparams;
        int minDelta;
        int maxDelta;

        public RangeEvaluatorFactory(String regex, int numberOfparams, int minDelta, int maxDelta) {
            super();
            this.regex = regex;
            this.numberOfparams = numberOfparams;
            this.minDelta = minDelta;
            this.maxDelta = maxDelta;
        }
    }

    public static class OneParameterEqualsIndexedEvaluator extends EqualsIndexedEvaluator {
        private OneParameterEqualsFactory oneParameterEqualsFactory;

        public OneParameterEqualsIndexedEvaluator(OneParameterEqualsFactory oneParameterEqualsFactory,
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
            return condition.getSourceCodeModule();
        }
    }

    public static class OneParameterEqualsIndexedEvaluatorV2 extends EqualsIndexedEvaluatorV2 {
        private OneParameterEqualsFactory oneParameterEqualsFactory;

        public OneParameterEqualsIndexedEvaluatorV2(OneParameterEqualsFactory oneParameterEqualsFactory,
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
            return condition.getSourceCodeModule();
        }
    }

    abstract static class EvaluatorFactory {

        IParameterDeclaration signatureParam;
        String expression;

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
        IParameterDeclaration conditionParam;
        RelationType relation;

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
        IParameterDeclaration conditionParam1;
        IParameterDeclaration conditionParam2;
        RelationType relation1;
        RelationType relation2;

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
