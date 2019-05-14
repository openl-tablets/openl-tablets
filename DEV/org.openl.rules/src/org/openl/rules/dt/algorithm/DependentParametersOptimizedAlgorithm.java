package org.openl.rules.dt.algorithm;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.regex.Pattern;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.*;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.meta.*;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.*;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.ITypeAdaptor;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
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

        EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(condition, signature);

        if (evaluatorFactory == null) {
            return null;
        }

        IOpenClass expressionType = evaluatorFactory.getExpressionType();

        IParameterDeclaration[] params = condition.getParams();

        switch (params.length) {

            case 1:
                IOpenClass paramType = params[0].getType();

                IOpenCast openCast = bindingContext.getCast(paramType, expressionType);

                if (openCast == null) {
                    String message = String.format(
                        "Can not convert from '%s' to '%s'. incompatible types comparison in '%s' condition",
                        paramType.getName(),
                        expressionType.getName(),
                        condition.getName());

                    throw new SyntaxNodeException(message, null, null, condition.getSourceCodeModule());
                }

                if (evaluatorFactory instanceof OneParameterEqualsFactory) {
                    return condition.getNumberOfEmptyRules(0) > 1
                            ? new OneParameterEqualsIndexedEvaluatorV2((OneParameterEqualsFactory) evaluatorFactory, openCast)
                            : new OneParameterEqualsIndexedEvaluator((OneParameterEqualsFactory) evaluatorFactory, openCast);
                } else {

                    IRangeAdaptor<?, ? extends Comparable<?>> adaptor = getRangeAdaptor(evaluatorFactory,
                        expressionType,
                        openCast);

                    if (adaptor == null) {
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    AConditionEvaluator rix = new SingleRangeIndexEvaluator(
                        (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor);
                    rix.setOptimizedSourceCode(evaluatorFactory.getExpression());
                    return rix;
                }

            case 2:

                IOpenClass paramType0 = params[0].getType();
                IOpenClass paramType1 = params[1].getType();

                if (paramType0.equals(paramType1)) {
                    IOpenCast cast = bindingContext.getCast(paramType0, expressionType);
                    if (cast == null) {
                        String message = String.format(
                            "Can not convert from '%s' to '%s'. incompatible types comparison in '%s' condition",
                            paramType0.getName(),
                            expressionType.getName(),
                            condition.getName());
                        throw new SyntaxNodeException(message, null, null, condition.getSourceCodeModule());
                    }
                    IRangeAdaptor<?, ? extends Comparable<?>> adaptor = getRangeAdaptor(evaluatorFactory,
                        expressionType,
                        cast);

                    if (adaptor == null) {
                        return null;
                    }

                    @SuppressWarnings("unchecked")
                    CombinedRangeIndexEvaluator rix = new CombinedRangeIndexEvaluator(
                        (IRangeAdaptor<Object, ? extends Comparable<Object>>) adaptor,
                        2);

                    rix.setOptimizedSourceCode(evaluatorFactory.getExpression());

                    return rix;
                }
                break;
        }

        return null;
    }

    private static IRangeAdaptor<?, ? extends Comparable<?>> getRangeAdaptor(EvaluatorFactory evaluatorFactory,
            IOpenClass paramType,
            IOpenCast openCast) {

        Class<?> typeClass = paramType.getInstanceClass();
        if (typeClass.equals(String.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.STRING, openCast);
        }

        if (typeClass.equals(byte.class) || typeClass.equals(Byte.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BYTE, openCast);
        }

        if (typeClass.equals(ByteValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BYTE_VALUE, openCast);
        }

        if (typeClass.equals(short.class) || typeClass.equals(Short.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.SHORT, openCast);
        }

        if (typeClass.equals(ShortValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.SHORT_VALUE, openCast);
        }

        if (typeClass.equals(int.class) || typeClass.equals(Integer.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.INT, openCast);
        }

        if (typeClass.equals(IntValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.INT_VALUE, openCast);
        }

        if (typeClass.equals(long.class) || typeClass.equals(Long.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.LONG, openCast);
        }

        if (typeClass.equals(LongValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.LONG_VALUE, openCast);
        }

        if (typeClass.equals(double.class) || typeClass.equals(Double.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DOUBLE, openCast);
        }

        if (typeClass.equals(DoubleValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DOUBLE_VALUE, openCast);
        }

        if (typeClass.equals(float.class) || typeClass.equals(Float.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.FLOAT, openCast);
        }

        if (typeClass.equals(FloatValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.FLOAT_VALUE, openCast);
        }

        if (typeClass.equals(BigInteger.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGINTEGER, openCast);
        }

        if (typeClass.equals(BigIntegerValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGINTEGER_VALUE, openCast);
        }

        if (typeClass.equals(BigDecimal.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGDECIMAL, openCast);
        }

        if (typeClass.equals(BigDecimalValue.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.BIGDECIMAL_VALUE, openCast);
        }

        if (typeClass.equals(Date.class)) {
            return new RelationRangeAdaptor<>(evaluatorFactory, ITypeAdaptor.DATE, openCast);
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
            throw new SyntaxNodeException("Can't parse array index", null, indexNode.getSyntaxNode());
        }

        if (indexNode.getTargetNode() != null) {
            if (indexNode.getTargetNode() instanceof FieldBoundNode) {
                return buildFieldName((FieldBoundNode) indexNode.getTargetNode()) + value;
            }
            if (indexNode.getTargetNode() instanceof IndexNode) {
                return value + buildFieldName((IndexNode) indexNode.getTargetNode());
            }
            throw new SyntaxNodeException("Can't parse array index", null, indexNode.getSyntaxNode());
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
            throw new SyntaxNodeException("Can't parse field name", null, field.getSyntaxNode());
        }
        return value;
    }

    private static String[] parseBinaryOpExpression(BinaryOpNode binaryOpNode) throws SyntaxNodeException {
        IBoundNode[] children = binaryOpNode.getChildren();
        if (children != null && children.length == 2 && children[0] instanceof FieldBoundNode && children[1] instanceof FieldBoundNode) {
            String[] ret = new String[3];
            if (binaryOpNode.getSyntaxNode().getType().endsWith("ge")) {
                ret[1] = ">=";
            } else if (binaryOpNode.getSyntaxNode().getType().endsWith("gt")) {
                ret[1] = ">";
            } else if (binaryOpNode.getSyntaxNode().getType().endsWith("le")) {
                ret[1] = "<=";
            } else if (binaryOpNode.getSyntaxNode().getType().endsWith("lt")) {
                ret[1] = "<";
            } else if (binaryOpNode.getSyntaxNode().getType().endsWith("eq")) {
                ret[1] = "==";
            } else {
                return null;
            }
            FieldBoundNode fieldBoundNode0 = (FieldBoundNode) children[0];
            FieldBoundNode fieldBoundNode1 = (FieldBoundNode) children[1];

            ret[0] = buildFieldName(fieldBoundNode0);
            ret[2] = buildFieldName(fieldBoundNode1);
            return ret;
        }
        return null;
    }

    private static String[] oneParameterExpressionParse(ICondition condition) throws SyntaxNodeException {
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
        throw new IllegalStateException("Condition method should be an instance of CompositeMethod!");
    }

    private static String[][] twoParameterExpressionParse(ICondition condition) throws SyntaxNodeException {
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
                            String[] ret0 = parseBinaryOpExpression(binaryOpNode0);
                            String[] ret1 = parseBinaryOpExpression(binaryOpNode1);

                            if (ret0 != null && ret1 != null) {
                                if ("==".equals(ret0[1]) || "==".equals(ret1[1])) {
                                    return null;
                                }
                                String[][] ret = new String[2][];
                                ret[0] = ret0;
                                ret[1] = ret1;
                                return ret;
                            }
                        }
                    }
                }
            }
            return null;
        }
        throw new IllegalStateException("Condition method should be an instance of CompositeMethod!");
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
                String[] parsedValues = oneParameterExpressionParse(condition);
                if (parsedValues == null) {
                    return null;
                }
                if ("==".equals(parsedValues[1])) {
                    return makeOneParameterEqualsFactory(parsedValues[0],
                        parsedValues[1],
                        parsedValues[2],
                        condition,
                        signature);
                } else {
                    OneParameterRangeFactory oneParameterRangefactory = makeOneParameterRangeFactory(parsedValues[0],
                        parsedValues[1],
                        parsedValues[2],
                        condition,
                        signature);
                    return oneParameterRangefactory;
                }
            case 2:
                String[][] parsedValuesTwoParameters = twoParameterExpressionParse(condition);
                if (parsedValuesTwoParameters == null) {
                    return null;
                }
                return makeTwoParameterRangeFactory(parsedValuesTwoParameters[0][0],
                    parsedValuesTwoParameters[0][1],
                    parsedValuesTwoParameters[0][2],
                    parsedValuesTwoParameters[1][0],
                    parsedValuesTwoParameters[1][1],
                    parsedValuesTwoParameters[1][2],
                    condition,
                    signature);
            default:
                return null;
        }

    }

    private static OneParameterEqualsFactory makeOneParameterEqualsFactory(String p1,
            String op,
            String p2,
            ICondition condition,
            IMethodSignature signature) {

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

    private static OneParameterRangeFactory makeOneParameterRangeFactory(String p1,
            String op,
            String p2,
            ICondition condition,
            IMethodSignature signature) throws SyntaxNodeException {

        IParameterDeclaration signatureParam = getParameter(p1, signature);

        if (signatureParam == null) {
            return makeOppositeOneParameterRangeFactory(p1, op, p2, condition, signature);
        }

        IParameterDeclaration conditionParam = condition.getParams()[0];

        if (!p2.equals(conditionParam.getName())) {
            return null;
        }

        RelationType relation = RelationType.findElement(op);

        if (relation == null) {
            throw SyntaxNodeExceptionUtils.createError("Could not find relation: " + op,
                condition.getSourceCodeModule());
        }

        if (p1.startsWith(signatureParam.getName() + "[") || p1.startsWith(signatureParam.getName() + ".") || p1
            .equals(signatureParam.getName())) {
            return new OneParameterRangeFactory(signatureParam, conditionParam, relation, p1);
        } else {
            return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                relation,
                signatureParam.getName() + "." + p1);
        }
    }

    private static TwoParameterRangeFactory makeTwoParameterRangeFactory(String p11,
            String op1,
            String p12,
            String p21,
            String op2,
            String p22,
            ICondition condition,
            IMethodSignature signature) throws SyntaxNodeException {

        RelationType rel1 = RelationType.findElement(op1);

        if (rel1 == null) {
            throw SyntaxNodeExceptionUtils.createError("Could not find relation: " + op1,
                condition.getSourceCodeModule());
        }

        if (!rel1.isLessThan()) {
            rel1 = RelationType.findElement(rel1.opposite);
            String tmp = p11;
            p11 = p12;
            p12 = tmp;
        }

        RelationType rel2 = RelationType.findElement(op2);
        if (rel2 == null) {
            throw SyntaxNodeExceptionUtils.createError("Could not find relation: " + op2,
                condition.getSourceCodeModule());
        }

        if (!rel2.isLessThan()) {
            rel2 = RelationType.findElement(rel2.opposite);
            String tmp = p21;
            p21 = p22;
            p22 = tmp;
        }

        if (p12.equals(p21)) {
            return makeTwoParameterRangeFactory(p11, rel1, p12, p21, rel2, p22, condition, signature);
        }

        if (p11.equals(p22)) {
            return makeTwoParameterRangeFactory(p21, rel2, p22, p11, rel1, p12, condition, signature);
        }

        return null;

    }

    private static TwoParameterRangeFactory makeTwoParameterRangeFactory(String p11,
            RelationType rel1,
            String p12,
            String p21,
            RelationType rel2,
            String p22,
            ICondition condition,
            IMethodSignature signature) {

        IParameterDeclaration signatureParam = getParameter(p12, signature);

        if (signatureParam == null) {
            return null;
        }

        IParameterDeclaration conditionParam1 = condition.getParams()[0];

        if (!p11.equals(conditionParam1.getName())) {
            return null;
        }

        IParameterDeclaration conditionParam2 = condition.getParams()[1];

        if (!p22.equals(conditionParam2.getName())) {
            return null;
        }

        if (p12.startsWith(signatureParam.getName() + "[") || p12.startsWith(signatureParam.getName() + ".") || p12
            .equals(signatureParam.getName())) {
            return new TwoParameterRangeFactory(signatureParam, conditionParam1, rel1, conditionParam2, rel2, p12);
        } else {
            return new TwoParameterRangeFactory(signatureParam,
                conditionParam1,
                rel1,
                conditionParam2,
                rel2,
                signatureParam.getName() + "." + p12);
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

    private static OneParameterRangeFactory makeOppositeOneParameterRangeFactory(String p1,
            String op,
            String p2,
            ICondition condition,
            IMethodSignature signature) throws SyntaxNodeException {

        IParameterDeclaration signatureParam = getParameter(p2, signature);

        if (signatureParam == null) {
            return null;
        }

        IParameterDeclaration conditionParam = condition.getParams()[0];

        if (!p1.equals(conditionParam.getName())) {
            return null;
        }

        RelationType relation = RelationType.findElement(op);

        if (relation == null) {
            throw SyntaxNodeExceptionUtils.createError("Could not find relation: " + op,
                condition.getSourceCodeModule());
        }

        String oppositeOp = relation.opposite;

        relation = RelationType.findElement(oppositeOp);

        if (relation == null) {
            throw SyntaxNodeExceptionUtils.createError("Could not find relation: " + oppositeOp,
                condition.getSourceCodeModule());
        }

        if (p2.startsWith(signatureParam.getName() + "[") || p2.startsWith(signatureParam.getName() + ".") || p2
            .equals(signatureParam.getName())) {
            return new OneParameterRangeFactory(signatureParam, conditionParam, relation, p2);
        } else {
            return new OneParameterRangeFactory(signatureParam,
                conditionParam,
                relation,
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
        GT(">", "<", false, Bound.LOWER);

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

        static RelationType findElement(String code) {
            RelationType[] all = values();
            for (int i = 0; i < all.length; i++) {
                if (code.equals(all[i].func)) {
                    return all[i];
                }
            }

            return null;
        }

        public Bound getIncBound() {
            return incBound;
        }

        public boolean isLessThan() {
            return lessThan;
        }

    }

    static class RelationRangeAdaptor<C extends Comparable<C>> implements IRangeAdaptor<Object, C> {
        EvaluatorFactory evaluatorFactory;
        ITypeAdaptor<Object, C> typeAdaptor;
        IOpenCast openCast;

        @SuppressWarnings("unchecked")
        RelationRangeAdaptor(EvaluatorFactory evaluatorFactory,
                ITypeAdaptor<? extends Object, C> typeAdaptor,
                IOpenCast openCast) {
            super();
            this.evaluatorFactory = evaluatorFactory;
            this.typeAdaptor = (ITypeAdaptor<Object, C>) typeAdaptor;
            this.openCast = openCast;
        }

        @Override
        public C getMax(Object param) {
            if (param == null) {
                return null;
            }
            if (evaluatorFactory.hasMax()) {
                if (openCast != null) {
                    param = openCast.convert(param);
                }
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
                if (openCast != null) {
                    param = openCast.convert(param);
                }
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
        int minDelta, maxDelta;

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
                IOpenCast openCast) {
            super(openCast);
            if (oneParameterEqualsFactory == null) {
                throw new IllegalArgumentException("parameterDeclaration");
            }
            this.oneParameterEqualsFactory = oneParameterEqualsFactory;
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
                                                  IOpenCast openCast) {
            super(openCast);
            if (oneParameterEqualsFactory == null) {
                throw new IllegalArgumentException("parameterDeclaration");
            }
            this.oneParameterEqualsFactory = oneParameterEqualsFactory;
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

    static abstract class EvaluatorFactory {

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
        RelationType relation1, relation2;

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
