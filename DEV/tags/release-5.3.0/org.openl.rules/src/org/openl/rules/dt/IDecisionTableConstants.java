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

    static final String CONDITION = "C", ACTION = "A", RULE = "RULE", RETURN = "RET";

    static final int INFO_COLUMN = 0, CODE_COLUMN = 1, PARAM_COLUMN = 2, PRESENTATION_COLUMN = 3, DATA_COLUMN = 4;

    /**
     * Value type constants
     */

    public static final int NA_VALUE = 0, BOOLEAN_VALUE = 1, PARAMETER_VALUE = 2, DEFAULT_VALUE = 3, // true
                                                                                                        // if
                                                                                                        // all
                                                                                                        // conditions
                                                                                                        // to
                                                                                                        // the
                                                                                                        // left,
                                                                                                        // except
                                                                                                        // for
                                                                                                        // special
                                                                                                        // cases
                                                                                                        // are
                                                                                                        // false
            NOT_VALUE = 4, // inverses previous non-special cell
            REPEAT_VALUE = 5; // same as previous non-special cell

}
