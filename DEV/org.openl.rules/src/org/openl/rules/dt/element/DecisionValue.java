/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt.element;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author snshor
 */
@RequiredArgsConstructor
public class DecisionValue {

    private static final int FALSE = 1;
    private static final int TRUE = 2;
    private static final int NA = 3;

    public static final DecisionValue FALSE_VALUE = new DecisionValue(FALSE, false, false);
    public static final DecisionValue TRUE_VALUE = new DecisionValue(TRUE, true, false);
    public static final DecisionValue NxA_VALUE = new DecisionValue(NA, true, true);

    @Getter
    private final int type;
    private final boolean booleanValue;
    @Getter
    private final boolean special;

    public boolean getBooleanValue() {
        return booleanValue;
    }

}
