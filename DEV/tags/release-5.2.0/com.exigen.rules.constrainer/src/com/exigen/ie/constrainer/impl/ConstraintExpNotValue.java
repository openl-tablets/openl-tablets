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
 * An implementation of the constraint: <code>IntExp != value</code>.
 */
public final class ConstraintExpNotValue extends ConstraintImpl
{
  // PRIVATE MEMBERS
  private IntExp	   _exp;
  private int	       _value;
  private Constraint _opposite;

  public ConstraintExpNotValue(IntExp exp, int value)
  {
    super(exp.constrainer());

    if(constrainer().showInternalNames())
    {
      _name = "("+exp.name()+"!="+value+")";
    }

    _exp = exp;
    _value = value;
  }

  public Goal execute() throws Failure
  {
    class ObserverExpNotValue extends Observer
    {
      public void update(Subject exp, EventOfInterest interest)
        throws Failure
      {
        //Debug.print("ObserverExpNotValue("+_exp+") "+interest);
        int value = ((IntEvent)interest).min();
        if (value == _value)
//					constrainer().fail("attempt to set a removed value "+value+" for "+exp);
          constrainer().fail();
      }

      public int subscriberMask()
      {
        return  EventOfInterest.VALUE;
      }

      public String toString()
      {
        return "ObserverExpNotValue:"+_exp.name()+"!="+_value;
      }

      public Object master()
      {
        return ConstraintExpNotValue.this;
      }




    } //~ ObserverExpNotValue


    if (_value == _exp.max())
      _exp.setMax(_value-1);
    else
    if (_value == _exp.min())
      _exp.setMin(_value+1);
    else
    {
      //Debug.print("attach ObserverExpNotValue("+_exp+","+_value+")");
      _exp.attachObserver(new ObserverExpNotValue());
    }

    return null;
  }

  public Constraint opposite()
  {
    if (_opposite == null)
      _opposite = new ConstraintExpEqualsValue(_exp,_value);
    return _opposite;
  }

  public IntBoolExp toIntBoolExp(){
    return _exp.ne(_value);
  }

  public String toString()
  {
    return _exp+"!="+_value;
  }

} //~ ConstraintExpNotValue
