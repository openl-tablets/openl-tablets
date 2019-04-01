/**
 * Created Feb 7, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.ie.constrainer.consistencyChecking.CDecisionTable;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.validator.IValidatedObject;

/**
 * @author snshor
 *
 */
public interface IDecisionTableValidatedObject extends IValidatedObject {

    IDecisionTable getDecisionTable();

    @Deprecated
    IConditionSelector getSelector();

    @Deprecated
    IConditionTransformer getTransformer();

    /**
     *
     * @return true if the {@link IDecisionTable} allows for ascending override (usually true for DT that return value)
     * @see CDecisionTable#isOverrideAscending()
     */

    boolean isOverrideAscending();

}
