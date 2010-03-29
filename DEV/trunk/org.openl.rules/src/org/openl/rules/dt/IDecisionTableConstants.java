/*
 * Created on Sep 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

/**
 * @author snshor
 *
 */
public interface IDecisionTableConstants {

    String CONDITION = "C";
    String ACTION = "A";
    String RULE = "RULE";
    String RETURN = "RET";

    int INFO_COLUMN = 0;
    int CODE_COLUMN = 1;
    int PARAM_COLUMN = 2;
    int PRESENTATION_COLUMN = 3;
    int DATA_COLUMN = 4;

    /**
     * Value type constants
     */
    int NA_VALUE = 0;
    int BOOLEAN_VALUE = 1;
    int PARAMETER_VALUE = 2;
    int DEFAULT_VALUE = 3; // true if all conditions to the left, except for special cases are false
    int NOT_VALUE = 4; // inverses previous non-special cell
    int REPEAT_VALUE = 5; // same as previous non-special cell

}
