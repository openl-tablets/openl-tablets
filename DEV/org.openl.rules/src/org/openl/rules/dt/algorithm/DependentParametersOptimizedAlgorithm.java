package org.openl.rules.dt.algorithm;

import java.nio.channels.IllegalSelectorException;
import java.util.Date;
import java.util.regex.Pattern;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.BinaryOpNode;
import org.openl.binding.impl.BlockNode;
import org.openl.binding.impl.FieldBoundNode;
import org.openl.binding.impl.IndexNode;
import org.openl.binding.impl.LiteralBoundNode;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.OneParameterEqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.RangeIndexedEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.ITypeAdaptor;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.impl.ParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class DependentParametersOptimizedAlgorithm {

    public static IConditionEvaluator makeEvaluator(ICondition condition, IMethodSignature signature) throws SyntaxNodeException {

        EvaluatorFactory evaluatorFactory = determineOptimizedEvaluationFactory(condition, signature);

        if (evaluatorFactory == null)
            return null;

        IOpenClass expressionType = evaluatorFactory.getExpressionType();

        IParameterDeclaration[] params = condition.getParams();

        switch (params.length) {

            case 1:
                IOpenClass paramType = params[0].getType();
                if (expressionType.equals(paramType) || expressionType.getInstanceClass()
                    .equals(paramType.getInstanceClass())) {
                    if (evaluatorFactory instanceof OneParameterEqualsFactory) {
                        return getOneParamEqualsEvaluator(evaluatorFactory, paramType);
                    } else {
                        return getOneParamRangeEvaluator(evaluatorFactory, paramType);
                    }
                }

                if (expressionType instanceof JavaOpenClass && ((JavaOpenClass) expressionType).equalsAsPrimitive(paramType)) {
                    if (evaluatorFactory instanceof OneParameterEqualsFactory) {
                        return getOneParamEqualsEvaluator(evaluatorFactory, paramType);
                    } else {
                        return getOneParamRangeEvaluator(evaluatorFactory, paramType);
                    }
                }
                break;

            case 2:

                IOpenClass paramType0 = params[0].getType();
                IOpenClass paramType1 = params[1].getType();

                if (expressionType == paramType0 && expressionType == paramType1) {

                    return getTwoParamRangeEvaluator(evaluatorFactory, expressionType);
                }

                break;
        }

        return null;
    }

    private static IConditionEvaluator getTwoParamRangeEvaluator(EvaluatorFactory evaluatorFactory, IOpenClass paramType) {
        IRangeAdaptor adaptor = getRangeAdaptor(evaluatorFactory, paramType);

        if (adaptor == null)
            return null;

        RangeIndexedEvaluator rix = new RangeIndexedEvaluator(adaptor, 2);

        rix.setOptimizedSourceCode(evaluatorFactory.signatureParam.getName());

        return rix;
    }

    private static IConditionEvaluator getOneParamEqualsEvaluator(EvaluatorFactory evaluatorFactory, IOpenClass paramType) {

        OneParameterEqualsFactory oneParameterEqualsFactory = (OneParameterEqualsFactory) evaluatorFactory;
        OneParameterEqualsIndexedEvaluator oneParameterEqualsIndexedEvaluator = new OneParameterEqualsIndexedEvaluator(oneParameterEqualsFactory.signatureParam);
        return oneParameterEqualsIndexedEvaluator;

    }
    
    private static IConditionEvaluator getOneParamRangeEvaluator(EvaluatorFactory evaluatorFactory, IOpenClass paramType) {

        IRangeAdaptor adaptor = getRangeAdaptor(evaluatorFactory, paramType);

        if (adaptor == null)
            return null;

        RangeIndexedEvaluator rix = new RangeIndexedEvaluator(adaptor, 1);

        rix.setOptimizedSourceCode(evaluatorFactory.signatureParam.getName());

        return rix;
    }

    private static IRangeAdaptor getRangeAdaptor(EvaluatorFactory evaluatorFactory, IOpenClass paramType) {

        if (paramType.getInstanceClass().equals(byte.class) || paramType.getInstanceClass().equals(Byte.class)) {
            return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.BYTE);
        }

        if (paramType.getInstanceClass().equals(short.class) || paramType.getInstanceClass().equals(Short.class)) {
            return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.SHORT);
        }

        if (paramType.getInstanceClass().equals(int.class) || paramType.getInstanceClass().equals(Integer.class)) {
            return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.INT);
        }

        
        if (paramType.getInstanceClass().equals(long.class) || paramType.getInstanceClass().equals(Long.class)) {
            return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.LONG);
        }

        if (paramType.getInstanceClass().equals(Date.class)) {
            return new RelationRangeAdaptor(evaluatorFactory, ITypeAdaptor.DATE);
        }

        return null;
    }

    static class RelationRangeAdaptor implements IRangeAdaptor {
        EvaluatorFactory evaluatorFactory;
        ITypeAdaptor typeAdaptor;

        public RelationRangeAdaptor(EvaluatorFactory evaluatorFactory, ITypeAdaptor typeAdaptor) {
            super();
            this.evaluatorFactory = evaluatorFactory;
            this.typeAdaptor = typeAdaptor;
        }

        @Override
        public Comparable getMax(Object param) {

            if (evaluatorFactory.hasMax()) {
                Comparable v = (Comparable) typeAdaptor.convert(param);
                if (evaluatorFactory.needsIncrement(Bound.UPPER))
                    v = (Comparable) typeAdaptor.increment(v);
                return v;
            }

            return (Comparable) typeAdaptor.getMaxBound();
        }

        @Override
        public Comparable getMin(Object param) {
            if (evaluatorFactory.hasMin()) {
                Comparable v = (Comparable) typeAdaptor.convert(param);
                if (evaluatorFactory.needsIncrement(Bound.LOWER))
                    v = (Comparable) typeAdaptor.increment(v);
                return v;
            }

            return (Comparable) typeAdaptor.getMinBound();
        }

        @Override
        public Comparable adaptValueType(Object value) {
            return (Comparable) typeAdaptor.convert(value);
        }

        @Override
        public boolean useOriginalSource() {
            return true;
        }

    }

    private static String buildFieldName(IndexNode indexNode){
        String value = "[";
        if (indexNode.getChildren().length == 1 && indexNode.getChildren()[0] instanceof LiteralBoundNode){
            LiteralBoundNode literalBoundNode = (LiteralBoundNode) indexNode.getChildren()[0];
            value = value + literalBoundNode.getValue().toString() + "]";
        }else{
            throw new IllegalSelectorException();
        }
        
        if (indexNode.getTargetNode() != null){
            if (indexNode.getTargetNode() instanceof FieldBoundNode){
                return buildFieldName((FieldBoundNode)indexNode.getTargetNode()) + value;
            }
            if (indexNode.getTargetNode() instanceof IndexNode){
                return value + buildFieldName((IndexNode)indexNode.getTargetNode());
            }
            throw new IllegalStateException();
        }
        return value;
    }

    private static String buildFieldName(FieldBoundNode field){
        String value = field.getFieldName();
        if (field.getTargetNode() != null){
            if (field.getTargetNode() instanceof FieldBoundNode){
                return buildFieldName((FieldBoundNode)field.getTargetNode()) + "." + value;
            }
            if (field.getTargetNode() instanceof IndexNode){
                return buildFieldName((IndexNode)field.getTargetNode()) + "." + value;
            }
            throw new IllegalStateException();
        }
        return value;
    }
    
    private static String[] parseBinaryOpExpression(BinaryOpNode binaryOpNode) {
        if (binaryOpNode.getChildren().length == 2 && binaryOpNode.getChildren()[0] instanceof FieldBoundNode && binaryOpNode.getChildren()[1] instanceof FieldBoundNode) {
            String[] ret = new String[3];
            if (binaryOpNode.getSyntaxNode().getType().endsWith("ge")) {
                ret[1] = ">=";
            }
            if (binaryOpNode.getSyntaxNode().getType().endsWith("gt")) {
                ret[1] = ">";
            }
            if (binaryOpNode.getSyntaxNode().getType().endsWith("le")) {
                ret[1] = "<=";
            }
            if (binaryOpNode.getSyntaxNode().getType().endsWith("lt")) {
                ret[1] = "<";
            }
            if (binaryOpNode.getSyntaxNode().getType().endsWith("eq")) {
                ret[1] = "==";
            }
            if (ret[1] == null) {
                return null;
            }
            FieldBoundNode fieldBoundNode0 = (FieldBoundNode) binaryOpNode.getChildren()[0];
            FieldBoundNode fieldBoundNode1 = (FieldBoundNode) binaryOpNode.getChildren()[1];

            ret[0] = buildFieldName(fieldBoundNode0);
            ret[2] = buildFieldName(fieldBoundNode1);
            return ret;
        }
        return null;
    }

    private static String[] oneParameterExpressionParse(ICondition condition) {
        if (condition.getMethod() instanceof CompositeMethod) {
            IBoundNode boundNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode) {
                BlockNode blockNode = (BlockNode) boundNode;
                if (blockNode.getChildren().length == 1 && blockNode.getChildren()[0] instanceof BlockNode) {
                    blockNode = (BlockNode) blockNode.getChildren()[0];
                    if (blockNode.getChildren().length == 1 && blockNode.getChildren()[0] instanceof BinaryOpNode) {
                        BinaryOpNode binaryOpNode = (BinaryOpNode) blockNode.getChildren()[0];
                        return parseBinaryOpExpression(binaryOpNode);
                    }
                }
            }
            return null;
        }
        throw new IllegalStateException("Condition method should be an instance of CompositeMethod!");
    }

    private static String[][] twoParameterExpressionParse(ICondition condition) {
        if (condition.getMethod() instanceof CompositeMethod) {
            IBoundNode boundNode = ((CompositeMethod) condition.getMethod()).getMethodBodyBoundNode();
            if (boundNode instanceof BlockNode) {
                BlockNode blockNode = (BlockNode) boundNode;
                if (blockNode.getChildren().length == 1 && blockNode.getChildren()[0] instanceof BlockNode) {
                    blockNode = (BlockNode) blockNode.getChildren()[0];
                    if (blockNode.getChildren().length == 1 && blockNode.getChildren()[0] instanceof BinaryOpNode) {
                        BinaryOpNode binaryOpNode = (BinaryOpNode) blockNode.getChildren()[0];
                        if (binaryOpNode.getSyntaxNode().getType().endsWith("and") && binaryOpNode.getChildren().length == 2 && binaryOpNode.getChildren()[0] instanceof BinaryOpNode && binaryOpNode.getChildren()[1] instanceof BinaryOpNode) {
                            BinaryOpNode binaryOpNode0 = (BinaryOpNode) binaryOpNode.getChildren()[0];
                            BinaryOpNode binaryOpNode1 = (BinaryOpNode) binaryOpNode.getChildren()[1];
                            String[] ret0 = parseBinaryOpExpression(binaryOpNode0);
                            String[] ret1 = parseBinaryOpExpression(binaryOpNode1);
                            if (ret0 != null && ret1 != null) {
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
    
    private static EvaluatorFactory determineOptimizedEvaluationFactory(ICondition condition, IMethodSignature signature) {
        IParameterDeclaration[] params = condition.getParams();

        String code = condition.getSourceCodeModule().getCode();
        if (code == null)
            return null;

        switch (params.length) {
            case 1:
                String[] parsedValues = oneParameterExpressionParse(condition);
                if (parsedValues == null)
                    return null;
                if (parsedValues[1] == "==") {
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
                if (parsedValuesTwoParameters == null)
                    return null;
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
            if (signatureParam == null){
                return null;
            }
            if (!p1.equals(conditionParam.getName()))
                return null;

            return new OneParameterEqualsFactory(signatureParam);
        }

        if (!p2.equals(conditionParam.getName()))
            return null;

        return new OneParameterEqualsFactory(signatureParam);
    }

    private static OneParameterRangeFactory makeOneParameterRangeFactory(String p1,
            String op,
            String p2,
            ICondition condition,
            IMethodSignature signature) {

        IParameterDeclaration signatureParam = getParameter(p1, signature);

        if (signatureParam == null)
            return makeOppositeOneParameterRangeFactory(p1, op, p2, condition, signature);

        IParameterDeclaration conditionParam = condition.getParams()[0];

        if (!p2.equals(conditionParam.getName()))
            return null;

        RelationType relation = RelationType.findElement(op);

        if (relation == null)
            throw new RuntimeException("Could not find relation: " + op);

        return new OneParameterRangeFactory(signatureParam, conditionParam, relation);

    }

    private static TwoParameterRangeFactory makeTwoParameterRangeFactory(String p11,
            String op1,
            String p12,
            String p21,
            String op2,
            String p22,
            ICondition condition,
            IMethodSignature signature) {

        RelationType rel1 = RelationType.findElement(op1);

        if (!rel1.isLessThan()) {
            rel1 = RelationType.findElement(rel1.opposite);
            String tmp = p11;
            p11 = p12;
            p12 = tmp;
        }

        RelationType rel2 = RelationType.findElement(op2);

        if (!rel2.isLessThan()) {
            rel2 = RelationType.findElement(rel2.opposite);
            String tmp = p21;
            p21 = p22;
            p22 = tmp;
        }

        if (p12.equals(p21))
            return makeTwoParameterRangeFactory(p11, rel1, p12, p21, rel2, p22, condition, signature);

        if (p11.equals(p22))
            return makeTwoParameterRangeFactory(p21, rel2, p22, p11, rel1, p12, condition, signature);

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

        if (signatureParam == null)
            return null;

        IParameterDeclaration conditionParam1 = condition.getParams()[0];

        if (!p11.equals(conditionParam1.getName()))
            return null;

        IParameterDeclaration conditionParam2 = condition.getParams()[1];

        if (!p22.equals(conditionParam2.getName()))
            return null;

        return new TwoParameterRangeFactory(signatureParam, conditionParam1, rel1, conditionParam2, rel2);

    }

    private static IParameterDeclaration getParameter(String pname, IMethodSignature signature) {

        for (int i = 0; i < signature.getNumberOfParameters(); i++) {
            if (pname.equals(signature.getParameterName(i))) {
                return new ParameterDeclaration(signature.getParameterType(i), pname);
            }
        }
        return null;
    }

    private static OneParameterRangeFactory makeOppositeOneParameterRangeFactory(String p1,
            String op,
            String p2,
            ICondition condition,
            IMethodSignature signature) {

        IParameterDeclaration signatureParam = getParameter(p2, signature);

        if (signatureParam == null)
            return null;

        IParameterDeclaration conditionParam = condition.getParams()[0];

        if (!p1.equals(conditionParam.getName()))
            return null;

        RelationType relation = RelationType.findElement(op);

        if (relation == null)
            throw new RuntimeException("Could not find relation: " + op);

        String oppositeOp = relation.opposite;

        relation = RelationType.findElement(oppositeOp);

        if (relation == null)
            throw new RuntimeException("Could not find relation: " + oppositeOp);

        return new OneParameterRangeFactory(signatureParam, conditionParam, relation);
    }

    static class RangeEvaluatorFactory {

        public RangeEvaluatorFactory(String regex, int numberOfparams, int minDelta, int maxDelta) {
            super();
            this.regex = regex;
            this.numberOfparams = numberOfparams;
            this.minDelta = minDelta;
            this.maxDelta = maxDelta;
        }

        Pattern pattern;
        String regex;
        int numberOfparams;
        int minDelta, maxDelta;
    }

    RangeEvaluatorFactory[] rangeFactories = { new RangeEvaluatorFactory(null, 0, 0, 0) };

    enum Bound {
        LOWER,
        UPPER
    }

    static abstract class EvaluatorFactory {

        IParameterDeclaration signatureParam;

        public EvaluatorFactory(IParameterDeclaration signatureParam) {
            super();
            this.signatureParam = signatureParam;
        }

        public abstract boolean hasMin();

        public abstract boolean hasMax();

        public abstract boolean needsIncrement(Bound bound);

        public IOpenClass getExpressionType() {
            return signatureParam.getType();
        }

    }

    static class OneParameterEqualsFactory extends EvaluatorFactory {
        public OneParameterEqualsFactory(IParameterDeclaration signatureParam) {
            super(signatureParam);
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

        public OneParameterRangeFactory(IParameterDeclaration signatureParam,
                IParameterDeclaration conditionParam,
                RelationType relation) {
            super(signatureParam);

            this.conditionParam = conditionParam;
            this.relation = relation;
        }

        RelationType relation;

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
                RelationType relation2) {
            super(signatureParam);

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
            if (bound == Bound.LOWER)
                return relation1 == RelationType.LT;
            return relation2 == RelationType.LE;
        }

    }

    enum RelationType {

        LT("<", ">", true, null),
        LE("<=", ">=", true, Bound.UPPER),
        GE(">=", "<=", false, null),
        GT(">", "<", false, Bound.LOWER);

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

        String func;
        String opposite;

        boolean lessThan;
        Bound incBound;

        static RelationType findElement(String code) {
            RelationType[] all = values();
            for (int i = 0; i < all.length; i++) {
                if (code.equals(all[i].func))
                    return all[i];
            }

            return null;
        }

    };

}
