/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.IBaseConditionEvaluator;
import org.openl.rules.dt.index.IRuleIndex;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IConditionEvaluator extends IBaseConditionEvaluator {

    int EQUALS_CONDITION_PRIORITY = 0;
    int ARRAY_CONDITION_PRIORITY = 0;
    int ARRAY2_CONDITION_PRIORITY = 10; // for ContainsInOrNotInArrayIndexedEvaluator
    int RANGE_CONDITION_PRIORITY = 90;

    int DEFAULT_CONDITION_PRIORITY = 100;
    int DECORATOR_CONDITION_PRIORITY = 100;

    IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env);

    IRuleIndex makeIndex(ICondition cond, IIntIterator it);

    boolean isIndexed();

    int countUniqueKeys(ICondition condition, IIntIterator it);

    // Added to support dependent parameters
    String getOptimizedSourceCode();

    void setOptimizedSourceCode(String code);

    int getPriority();
}
