/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.validator.IValidatedObject;

/**
 * @author snshor
 *
 */
public interface IDTValidatedObject extends IValidatedObject
{
	public DecisionTable getDT();
		
	public IConditionSelector getSelector();
	
	public IConditionTransformer getTransformer();
	
	
}
