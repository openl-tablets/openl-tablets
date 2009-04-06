package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constraint;
import com.exigen.ie.constrainer.ConstraintImpl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.IntBoolExp;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;

///////////////////////////////////////////////////////////////////////////////
/*
 * Copyright Exigen Group 1998, 1999, 2000
 * 320 Amboy Ave., Metuchen, NJ, 08840, USA, www.exigengroup.com
 *
 * The copyright to the computer program(s) herein
 * is the property of Exigen Group, USA. All rights reserved.
 * The program(s) may be used and/or copied only with
 * the written permission of Exigen Group
 * or in accordance with the terms and conditions
 * stipulated in the agreement/contract under which
 * the program(s) have been supplied.
 */
///////////////////////////////////////////////////////////////////////////////

/**
 * An implementation of the constraint: <code>IntExp1 <= IntExp2</code>.
 */
public final class ConstraintExpLessExp extends ConstraintImpl
{
  class ObserverExp2Max extends Observer
  {
    public void update(Subject exp, EventOfInterest interest)
      throws Failure
    {
      //Debug.print("ObserverExp2Max("+_exp1+","+_exp2+") "+interest);
      _exp1.setMax(_exp2.max()-_offset); // may fail
    }

    public int subscriberMask()
    {
       return EventOfInterest.MAX;
    }

    public String toString()
    {
      return _name+"(max:"+_exp2.name()+")";
    }

    public Object master()
    {
      return ConstraintExpLessExp.this;
    }

  } //~ ObserverExp2Max

  class ObserverExp1Min extends Observer
  {
    public void update(Subject exp, EventOfInterest interest)
      throws Failure
    {
      //Debug.print("ObserverExp1Min("+_exp1+","+_exp2+") "+interest);
      _exp2.setMin(_exp1.min()+_offset); // may fail
    }

    public int subscriberMask()
    {
       return EventOfInterest.MIN;
    }

    public String toString()
    {
      return _name+"(min:"+_exp1.name()+")";
    }

    public Object master()
    {
      return ConstraintExpLessExp.this;
    }

  } //~ ObserverExp1Min

  // PRIVATE MEMBERS
  private IntExp	_exp1;
  private IntExp	_exp2;
  private int		  _offset;
  private Constraint _opposite;

  /**
   *  exp1 <= exp2 + offset
   */

  public ConstraintExpLessExp(IntExp exp1, IntExp exp2, int offset)
  {
    super(exp1.constrainer());

    _exp1 = exp1;
    _exp2 = exp2;
    _offset = offset;
    _opposite = null;

    if (constrainer().showInternalNames())
    {
      if(offset == 0)
        _name = "(" + exp1.name() + "<=" + exp2.name() + ")";
      else if(offset>0)
        _name = "(" + exp1.name() + "<=" + exp2.name() + "+" + offset + ")";
      else
        _name = "(" + exp1.name() + "<=" + exp2.name() + offset + ")";
    }
  }

  public Goal execute() throws Failure
  {
    _exp1.setMax(_exp2.max()-_offset); // may fail
    _exp1.attachObserver(new ObserverExp1Min());

    _exp2.setMin(_exp1.min()+_offset); // may fail
    _exp2.attachObserver(new ObserverExp2Max());
    return null;
  }

  //  x < y + offset => x >= y + offset => x - offset + 1 > y => x + (-offset +1) > y
  public Constraint opposite()
  {
    if (_opposite == null)
      _opposite = new ConstraintExpLessExp(_exp2, _exp1, -_offset+1);
    return _opposite;
  }

  public boolean isLinear(){
    return (_exp1.isLinear() && _exp2.isLinear());
  }

  public IntBoolExp toIntBoolExp(){
    return _exp1.le(_exp2.add(_offset));
  }

  public String toString()
  {
    return _exp1+"<"+_exp2;
  }



} //~ ConstraintExpLessExp
