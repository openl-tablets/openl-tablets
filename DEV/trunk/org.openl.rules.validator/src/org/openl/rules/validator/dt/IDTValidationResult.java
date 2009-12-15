/**
 * Created Feb 7, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.rules.dt.DTOverlapping;
import org.openl.rules.dt.DTUncovered;
import org.openl.rules.dt.DecisionTable;
import org.openl.rules.validator.IValidationResult;

/**
 * @author snshor
 *
 */
public interface IDTValidationResult extends IValidationResult {
    DecisionTable getDT();

    DTOverlapping[] getOverlappings();

    DTUncovered[] getUncovered();
}
