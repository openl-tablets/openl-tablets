/**
 * Created Jul 11, 2007
 */
package org.openl.rules.dt;

import org.openl.domain.IIntIterator;
import org.openl.domain.IIntSelector;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public interface IDTConditionEvaluator
{

	IIntSelector getSelector(IDTCondition condition, Object target, Object[] dtparams, IRuntimeEnv env);

	
	public ADTRuleIndex makeIndex(Object[][] indexedparams, IIntIterator it);


	/**
	 * @return
	 */
	boolean isIndexed();

	
}
