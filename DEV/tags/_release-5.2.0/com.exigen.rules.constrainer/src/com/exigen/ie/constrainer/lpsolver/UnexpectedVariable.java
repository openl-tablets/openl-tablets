package com.exigen.ie.constrainer.lpsolver;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2002</p>
 * <p>Company: </p>
 * @author unascribed
 * @version 1.0
 */

import com.exigen.ie.constrainer.Expression;
import com.exigen.ie.constrainer.Failure;

public class UnexpectedVariable extends Failure {
  Expression _exp = null;

  public UnexpectedVariable(Expression exp) {
    _exp  = exp;
  }

  public UnexpectedVariable(Expression exp, String str){
    super(str);
    _exp = exp;
  }

  public Expression getExp(){
    return _exp;
  }

  public Throwable fillInStackTrace()
  {
    return this;
  }

}