package org.openl.rules.dt.element;

import org.openl.binding.impl.cast.IOpenCast;
import org.openl.rules.dt.IBaseCondition;
import org.openl.rules.dt.algorithm.evaluator.IConditionEvaluator;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.types.IMethodCaller;
import org.openl.types.impl.CompositeMethod;
import org.openl.vm.IRuntimeEnv;

public interface ICondition extends IBaseCondition, IDecisionRow {

    DecisionValue calculateCondition(int col, Object target, Object[] dtParams, IRuntimeEnv env);

    void setConditionEvaluator(IConditionEvaluator iConditionEvaluator);

    void setEvaluator(IMethodCaller iMethodCaller);

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

    /**
 * Sets the flag indicating whether the condition is dependent on parameters from other columns.
 *
 * @param dependentOnOtherColumnsParams true if the condition depends on parameters from other columns, false otherwise
 */
void setDependentOnOtherColumnsParams(boolean dependentOnOtherColumnsParams);

    /**
 * Sets the comparison cast for condition evaluation.
 *
 * @param comparisonCast the cast mechanism applied during comparison operations in condition evaluation
 */
void setComparisonCast(IOpenCast comparisonCast);

    /**
 * Retrieves the static method associated with this condition.
 *
 * @return the static method as a CompositeMethod
 */
CompositeMethod getStaticMethod();

    /**
 * Retrieves the source code module for the index.
 *
 * @return the index source code module
 */
IOpenSourceCodeModule getIndexSourceCodeModule();

    /**
 * Retrieves the composite method representing the index functionality for this condition.
 *
 * @return the composite index method associated with the condition
 */
CompositeMethod getIndexMethod();
}
