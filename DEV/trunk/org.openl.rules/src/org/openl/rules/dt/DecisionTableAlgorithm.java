/*
 * Created on Nov 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.dt;

import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class DecisionTableAlgorithm {
    
    @Deprecated
    private static final int COLUMN_MODE = 0, ROW_MODE = 1;

    private int nCond;
    private int nCol;
    private IDecisionValue[][] table;
    private IDTCondition[] elements;

    private Object[] dtparams;

    private Object target;

    private IRuntimeEnv env;

    public DecisionTableAlgorithm(int nCond, int nCol, IDTCondition[] elements, Object target, Object[] dtparams,
            IRuntimeEnv env) {
        this.nCond = nCond;
        this.nCol = nCol;
        this.elements = elements;
        this.target = target;
        this.dtparams = dtparams;
        this.env = env;
        table = new IDecisionValue[nCond][nCol];
    }

    public boolean calcColumn(int col) {
        for (int j = 0; j < nCond; j++) {
            IDecisionValue value = getDecisionValue(col, j);
            if (!value.getBooleanValue()) {
                return false;
            }
        }
        return true;
    }

    @Deprecated
    public boolean[] calculateTable() {
        boolean[] res = new boolean[nCol];

        for (int i = 0; i < res.length; i++) {
            res[i] = calcColumn(i);
        }
        return res;
    }

    private IDecisionValue getDecisionValue(int col, int row) {
        IDecisionValue value = table[row][col];
        if (value == null) {
            value = elements[row].calculateCondition(col, target, dtparams, env);
        }
        return value;
    }

}
