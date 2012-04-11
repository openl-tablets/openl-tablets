package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.Constraint;
import com.exigen.ie.constrainer.ConstraintImpl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Goal;
import com.exigen.ie.constrainer.IntBoolExp;
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

//
//: ConstraintExpEqualsValue.java
//
/**
 * An implementation of the constraint: <code>FloatExp == value</code>.
 */
public final class ConstraintFloatExpEqualsValue extends ConstraintImpl
{
  // PRIVATE MEMBERS
  private FloatExp    _exp;
  private double      _value;
  private Constraint  _opposite;

  public ConstraintFloatExpEqualsValue(FloatExp exp, double value)
  {
    super(exp.constrainer());
    _exp = exp;
    _value = value;

    if(constrainer().showInternalNames())
    {
      _name = "("+exp.name()+"="+value+")";
    }

  }

  public Goal execute() throws Failure
  {
    class ObserverFloatEqualValue extends Observer
    {
      public void update(Subject exp, EventOfInterest interest)	throws Failure
      {
        //Debug.on();Debug.print("ObserverFloatEqualValue: "+interest);Debug.off();
        FloatEvent event = (FloatEvent)interest;
        if (FloatCalc.gt (_value, event.max ()) || FloatCalc.gt (event.min (), _value)) {
          exp.constrainer().fail("from ObserverFloatEqualValue");
        }
        _exp.setValue(_value);
      }

      public int subscriberMask () {
         return EventOfInterest.VALUE | EventOfInterest.MINMAX;
      }
      public String toString() {
        return "ObserverFloatEqualValue";
      }
      public Object master () {
        return ConstraintFloatExpEqualsValue.this;
      }
    } //~ ObserverFloatEqualValue
    _exp.setValue (_value); // may fail
    _exp.attachObserver (new ObserverFloatEqualValue());
    return null;
  }
  public Constraint opposite()
  {
    if (_opposite == null)
      _opposite = new ConstraintFloatExpNotValue(_exp,_value);
    return _opposite;
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public IntBoolExp toIntBoolExp(){
    return _exp.eq(_value);
  }

  public String toString()
  {
    return _exp+"="+_value;
  }

} //~ ConstraintFloatExpEqualsValue
