/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

/**
 * @author snshor
 *
 */
public interface IDecisionValue {
    
    int UNDEFINED = 0;
    int FALSE = 1;
    int TRUE = 2;
    int NA = 3;
    int SPECIAL_FALSE = 4;
    int SPECIAL_TRUE = 5;

    IDecisionValue UNDEFINED_VALUE = new DecisionValue(UNDEFINED, true, true);
    IDecisionValue FALSE_VALUE = new DecisionValue(FALSE, false, false);
    IDecisionValue TRUE_VALUE = new DecisionValue(TRUE, true, false);
    IDecisionValue NxA_VALUE = new DecisionValue(NA, true, true);
    IDecisionValue SPECIAL_FALSE_VALUE = new DecisionValue(SPECIAL_FALSE, false, true);
    IDecisionValue SPECIAL_TRUE_VALUE = new DecisionValue(SPECIAL_TRUE, true, true);

    boolean getBooleanValue();

    int getType();

    boolean isSpecial();

    class DecisionValue implements IDecisionValue {
        private int type;
        private boolean booleanValue;

        private boolean special;

        public DecisionValue(int type, boolean booleanValue, boolean special) {
            this.type = type;
            this.booleanValue = booleanValue;
            this.special = special;
        }
       
        public boolean getBooleanValue() {
            return booleanValue;
        }

        public int getType() {
            return type;
        }

        public boolean isSpecial() {
            return special;
        }

    }
}
