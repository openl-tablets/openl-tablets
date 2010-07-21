package org.openl.ie.simplex;

import java.util.HashMap;

/**
 * <p>
 * Title:
 * </p>
 * <p>
 * Description:
 * </p>
 * <p>
 * Copyright: Copyright (c) 2002
 * </p>
 * <p>
 * Company:
 * </p>
 *
 * @author unascribed
 * @version 1.0
 */

public class Status {
    static HashMap dictionary = new HashMap();

    static {
        int i = org.openl.ie.simplex.DualErrorCodes.LPX_D_FEAS;
        i = org.openl.ie.simplex.IPSErrorCodes.LPX_T_OPT;
        i = org.openl.ie.simplex.LPErrorCodes.LPX_FEAS;
        i = org.openl.ie.simplex.MIPErrorCodes.LPX_I_FEAS;
        i = org.openl.ie.simplex.PrimalErrorCodes.LPX_P_INFEAS;
        i = org.openl.ie.simplex.SolutionErrorCodes.LPX_E_EMPTY;
        i = org.openl.ie.simplex.VarInfo.LPX_BS;
        i = org.openl.ie.simplex.VarKind.INT_VAR;
        i = org.openl.ie.simplex.VarType.LPX_DB;
    }

    static public String translate(int status) {
        String str = (String) dictionary.get(new Integer(status));
        if (str == null) {
            str = "Unknown status!";
        }
        return str;
    }

    private Status() {
    }

}