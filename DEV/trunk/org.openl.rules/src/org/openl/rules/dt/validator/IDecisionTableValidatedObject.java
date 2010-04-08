/**
 * Created Feb 7, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.rules.dt.DecisionTable;
import org.openl.rules.validator.IValidatedObject;

/**
 * @author snshor
 *
 */
public interface IDecisionTableValidatedObject extends IValidatedObject {
    
    DecisionTable getDecisionTable();

    @Deprecated
    IConditionSelector getSelector();

    @Deprecated
    IConditionTransformer getTransformer();

}
