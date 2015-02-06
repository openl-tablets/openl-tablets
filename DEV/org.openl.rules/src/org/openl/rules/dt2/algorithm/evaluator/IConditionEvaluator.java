/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt2.algorithm.evaluator;

import org.openl.domain.IDomain;
import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.rules.dt2.element.ICondition;
import org.openl.rules.dt2.index.ARuleIndex;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IConditionEvaluator {

    IOpenSourceCodeModule getFormalSourceCode(ICondition condition);

    IIntSelector getSelector(ICondition condition, Object target, Object[] dtparams, IRuntimeEnv env);

    ARuleIndex makeIndex(Object[][] indexedparams, IIntIterator it);

    boolean isIndexed();
    
    IDomain<? extends Object> getRuleParameterDomain(ICondition condition) throws DomainCanNotBeDefined;
    
    IDomain<? extends Object> getConditionParameterDomain(int paramIdx, ICondition condition) throws DomainCanNotBeDefined;

    //Added to support dependent parameters
	String getOptimizedSourceCode();

	void setOptimizedSourceCode(String code);
}
