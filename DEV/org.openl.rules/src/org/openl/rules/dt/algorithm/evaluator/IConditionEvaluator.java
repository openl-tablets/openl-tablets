/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt.algorithm.evaluator;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.index.ARuleIndex;
import org.openl.rules.dt.IBaseConditionEvaluator;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IConditionEvaluator extends IBaseConditionEvaluator {



    IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env);

    ARuleIndex makeIndex(ICondition cond, IIntIterator it);

    boolean isIndexed();
    
    int countUniqueKeys(ICondition condition, IIntIterator it);

    //Added to support dependent parameters
	String getOptimizedSourceCode();

	void setOptimizedSourceCode(String code);
}
