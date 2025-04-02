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
 * Sets a flag that indicates whether the condition depends on parameters from other columns.
 *
 * @param dependentOnOtherColumnsParams true if the condition depends on other columns' parameters; false otherwise
 */
void setDependentOnOtherColumnsParams(boolean dependentOnOtherColumnsParams);

    /**
 * Sets the comparison cast used for type conversion during the evaluation of the condition.
 *
 * @param comparisonCast the cast mechanism applied to convert values for comparison
 */
void setComparisonCast(IOpenCast comparisonCast);

    /**
 * Retrieves the composite static method associated with the condition.
 *
 * @return the CompositeMethod representing the condition's static method.
 */
CompositeMethod getStaticMethod();

    /**
 * Returns the source code module for the index.
 *
 * @return the index source code module
 */
IOpenSourceCodeModule getIndexSourceCodeModule();

    /**
 * Retrieves the composite method that represents the index method associated with the condition.
 *
 * @return the CompositeMethod for the condition's index method
 */
CompositeMethod getIndexMethod();
}
