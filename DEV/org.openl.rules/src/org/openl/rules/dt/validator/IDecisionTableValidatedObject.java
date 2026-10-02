/**
 * Created Feb 7, 2007
 */
package org.openl.rules.dt.validator;

import org.openl.ie.constrainer.DTChecker;
import org.openl.rules.dt.IDecisionTable;
import org.openl.rules.validator.IValidatedObject;

/**
 * @author snshor
 */
public interface IDecisionTableValidatedObject extends IValidatedObject {

    IDecisionTable getDecisionTable();

    /**
     * @deprecated It will be removed without replacement.
     */
    // The decision table validator still reads the condition transformer here; it has no replacement yet.
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "5.5.0")
    IConditionTransformer getTransformer();

    /**
     * Returns whether a rule of the {@link IDecisionTable} is applied before the rules below it: usually the case for a
     * decision table that returns a value.
     *
     * @see DTChecker
     */

    boolean isOverrideAscending();

}
