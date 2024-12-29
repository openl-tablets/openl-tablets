package org.openl.rules.dt.element;

import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.Invokable;
import org.openl.types.IMethodSignature;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    void setConditionEvaluator(IConditionEvaluator iConditionEvaluator);

    void setEvaluator(Invokable iMethodCaller);

    boolean isDependentOnInputParams();

    int getNumberOfEmptyRules(int paramIndex);

    IOpenSourceCodeModule getUserDefinedExpressionSource();

    /**
     * Identifier to detect whatever condition parameter is used in expression
     *
     * @param conditionParametersUsed {@code true} when it's used, otherwise false
     */
    void setConditionParametersUsed(boolean conditionParametersUsed);

    boolean isRuleIdOrRuleNameUsed();

    void setRuleIdOrRuleNameUsed(boolean ruleIdOrRuleNameUsed);

    boolean isDependentOnOtherColumnsParams();

    void setDependentOnOtherColumnsParams(boolean dependentOnOtherColumnsParams);

    void setComparisonCast(IOpenCast comparisonCast);

    CompositeMethod getStaticMethod();

    IOpenSourceCodeModule getIndexSourceCodeModule();

    CompositeMethod getIndexMethod();

    boolean optimizeExpression(IMethodSignature signature, OpenL openl, IBindingContext bindingContext);

    void resetOptimizedExpression();

    boolean isOptimizedExpression();
}
