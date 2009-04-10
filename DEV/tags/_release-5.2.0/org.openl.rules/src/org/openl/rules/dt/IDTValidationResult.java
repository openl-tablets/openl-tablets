/**
 * Created Feb 7, 2007
 */
package org.openl.rules.dt;

import org.openl.validate.IValidationResult;


/**
 * @author snshor
 *
 */
public interface IDTValidationResult extends IValidationResult
{
	DecisionTable getDT();
	
	DTOverlapping[] getOverlappings();
	
	DTUncovered[] getUncovered();
}
