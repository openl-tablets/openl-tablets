package com.exigen.ie.simplex;
import java.util.HashMap;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

public class Status {
  static HashMap dictionary = new HashMap();


  static{
    int i = com.exigen.ie.simplex.DualErrorCodes.LPX_D_FEAS;
      i = com.exigen.ie.simplex.IPSErrorCodes.LPX_T_OPT;
      i = com.exigen.ie.simplex.LPErrorCodes.LPX_FEAS;
      i = com.exigen.ie.simplex.MIPErrorCodes.LPX_I_FEAS;
      i = com.exigen.ie.simplex.PrimalErrorCodes.LPX_P_INFEAS;
      i = com.exigen.ie.simplex.SolutionErrorCodes.LPX_E_EMPTY;
      i = com.exigen.ie.simplex.VarInfo.LPX_BS;
      i = com.exigen.ie.simplex.VarKind.INT_VAR;
      i = com.exigen.ie.simplex.VarType.LPX_DB;
  }

  private Status() {
  }


  static public String translate(int status){
    String str = (String)dictionary.get(new Integer(status));
    if (str == null){
      str = "Unknown status!";
    }
    return str;
  }

}