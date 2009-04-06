package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>(IntExp == value)</code>.
 */
public final class IntBoolExpEqValue extends IntBoolExpForSubject
{
  private IntExp _exp;
  private int    _value;

  final class ObserverMinMax extends Observer
  {
    public int subscriberMask()
    {
      return ALL;
    }

    public void update(Subject exp, EventOfInterest interest)
                    throws Failure
    {
      setDomainMinMax();
    }

    public Object master()
    {
      return IntBoolExpEqValue.this;
    }

  } // ~ObserverMinMax


  public IntBoolExpEqValue(IntExp exp, int value)
  {
    super(exp.constrainer());

    _exp = exp;
    _value = value;

    if (constrainer().showInternalNames())
    {
      _name = "(" + exp.name() + "==" + value + ")";
    }

    setDomainMinMaxSafe();

    _exp.attachObserver(new ObserverMinMax());
  }

  protected boolean isSubjectTrue()
  {
    return _exp.min() == _value &&
           _exp.max() == _value;
  }

  protected boolean isSubjectFalse()
  {
    return !_exp.contains(_value);
  }

  protected void setSubjectTrue() throws Failure
  {
    _exp.setValue(_value);
  }

  protected void setSubjectFalse() throws Failure
  {
    _exp.removeValue(_value);
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return (_exp.neg().add(_value)).calcCoeffs(map, factor);
  }

//  public Constraint asConstraint()
//  {
//    return _exp.equals(_value);
//  }

} // ~IntBoolExpEqValue
