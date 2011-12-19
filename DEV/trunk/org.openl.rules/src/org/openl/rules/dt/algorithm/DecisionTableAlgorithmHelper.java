package org.openl.rules.dt.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.dt.algorithm.evaluator.ContainsInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.ContainsInOrNotInArrayIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.DefaultConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.EqualsIndexedEvaluator;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.rules.dt.algorithm.evaluator.RangeIndexedEvaluator;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.type.BooleanAdaptorFactory;
import org.openl.rules.dt.type.BooleanTypeAdaptor;
import org.openl.rules.dt.type.DoubleRangeAdaptor;
import org.openl.rules.dt.type.IRangeAdaptor;
import org.openl.rules.dt.type.IntRangeAdaptor;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenClass;

public class DecisionTableAlgorithmHelper {

    public static ArrayList<Object[][]> getIndexedParameters(IConditionEvaluator[] evaluators, DecisionTable decisionTable) throws SyntaxNodeException {
        ArrayList<Object[][]> params = new ArrayList<Object[][]>();
    
        for (int i = 0; i < evaluators.length; i++) {
    
            if (evaluators[i].isIndexed()) {
                /** Precalculate the indexed parameters.
                    For further usage in the optimized DT algorithm
                */
                Object[][] values = decisionTable.getConditionRows()[i].getParamValues();
                Object[][] precalculatedParams = prepareIndexedParams(values, decisionTable);
                params.add(precalculatedParams);
            } else {
                break;
            }
        }
        return params;
    }
    
    public static Object[][] prepareIndexedParams(Object[][] params, DecisionTable decisionTable) throws SyntaxNodeException {

        Object[][] indexedParams = new Object[params.length][];

        for (int i = 0; i < params.length; i++) {

            if (params[i] == null) {
                indexedParams[i] = null;
            } else {

                Object[] values = new Object[params[i].length];
                indexedParams[i] = values;

                for (int j = 0; j < values.length; j++) {

                    Object value = params[i][j];

                    if (value instanceof IOpenMethod) {
                        throw SyntaxNodeExceptionUtils.createError("Can not index conditions with formulas",
                            decisionTable.getSyntaxNode());
                    }
                    values[j] = value;
                }
            }
        }

        return indexedParams;
    }
    
    
    // TODO to do - fix _NO_PARAM_ issue
    
    @SuppressWarnings("unchecked")
    public static IConditionEvaluator makeEvaluator(ICondition condition, IOpenClass methodType) throws SyntaxNodeException {

        IParameterDeclaration[] params = condition.getParams();

        switch (params.length) {

            case 1:
                IOpenClass paramType = params[0].getType();

                if (methodType.equals(paramType) || methodType.getInstanceClass().equals(paramType.getInstanceClass())) {
                    return new EqualsIndexedEvaluator();
                }
                
                if (methodType instanceof JavaOpenClass && ((JavaOpenClass) methodType).equalsAsPrimitive(paramType)) {
                    return new EqualsIndexedEvaluator();
                }


                IAggregateInfo aggregateInfo = paramType.getAggregateInfo();

                if (aggregateInfo.isAggregate(paramType) && aggregateInfo.getComponentType(paramType)
                    .isAssignableFrom(methodType)) {

                    return new ContainsInArrayIndexedEvaluator();
                }

                IRangeAdaptor<Object, Object> rangeAdaptor = getRangeAdaptor(methodType, paramType);

                if (rangeAdaptor != null) {
                    return new RangeIndexedEvaluator(rangeAdaptor);
                }

                if (JavaOpenClass.BOOLEAN.equals(methodType) || JavaOpenClass.getOpenClass(Boolean.class).equals(methodType)) {
                    return new DefaultConditionEvaluator();

                }

                break;

            case 2:

                IOpenClass paramType0 = params[0].getType();
                IOpenClass paramType1 = params[1].getType();

                if (methodType == paramType0 && methodType == paramType1) {

                    Class<?> clazz = methodType.getInstanceClass();

                    if (clazz != int.class && clazz != long.class && clazz != double.class && clazz != float.class && !Comparable.class.isAssignableFrom(clazz)) {

                        String message = String.format("Type '%s' is not Comparable", methodType.getName());

                        throw SyntaxNodeExceptionUtils.createError(message, null, null, condition.getSourceCodeModule());
                    }

                    return new RangeIndexedEvaluator(null);
                }

                aggregateInfo = paramType1.getAggregateInfo();

                if (aggregateInfo.isAggregate(paramType1) && aggregateInfo.getComponentType(paramType1) == methodType) {

                    BooleanTypeAdaptor booleanTypeAdaptor = BooleanAdaptorFactory.getAdaptor(paramType0);

                    if (booleanTypeAdaptor != null) {
                        return new ContainsInOrNotInArrayIndexedEvaluator(booleanTypeAdaptor);
                    }
                }

                break;
        }

        List<String> names = new ArrayList<String>();

        for (IParameterDeclaration parameterDeclaration : params) {

            String name = parameterDeclaration.getType().getName();
            names.add(name);
        }

        String parametersString = StringUtils.join(names, ",");

        String message = String.format("Can not make a Condition Evaluator for parameter %s and [%s]",
            methodType.getName(),
            parametersString);

        throw SyntaxNodeExceptionUtils.createError(message, null, null, condition.getSourceCodeModule());
    }
    
    private static IRangeAdaptor getRangeAdaptor(IOpenClass methodType, IOpenClass paramType) {
        if (ClassUtils.isAssignable(methodType.getInstanceClass(), Number.class, true)) {
            if (org.openl.rules.helpers.IntRange.class.equals(paramType.getInstanceClass())) {
                return new IntRangeAdaptor();
            } else if (org.openl.rules.helpers.DoubleRange.class.equals(paramType.getInstanceClass())) {
                return new DoubleRangeAdaptor();
            }
        }
        return null;
    }

}
