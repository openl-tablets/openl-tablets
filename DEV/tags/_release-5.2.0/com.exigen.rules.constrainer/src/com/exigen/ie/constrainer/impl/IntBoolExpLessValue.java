package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.Constrainer;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>(IntExp < value)</code>.
 */
public class IntBoolExpLessValue extends IntBoolExpForSubject
{
  protected IntExp _left;
  protected int _right;
  private Observer _observer;

  class ObserverMinMax extends Observer
  {
    public int subscriberMask()
    {
      return MIN | MAX;
    }

    public void update(Subject subject, EventOfInterest interest)
                    throws Failure
    {
      setDomainMinMax();
    }

    public Object master()
    {
      return IntBoolExpLessValue.this;
    }

  } // ~ObserverMinMax

  public IntBoolExpLessValue(IntExp left, int right)
  {
    this(left, right, left.constrainer(), "");
  }

  public IntBoolExpLessValue(IntExp left, int right, Constrainer c, String name)
  {
    super(c, name);

    _left = left;
    _right = right;

    if(constrainer().showInternalNames())
    {
      _name = "("+left.name()+"<"+right+")";
    }
    setDomainMinMaxSafe();

    _observer = new ObserverMinMax();
    _left.attachObserver(_observer);
  }

  protected boolean isSubjectTrue()
  {
    return _left.max() < _right;
  }

  protected boolean isSubjectFalse()
  {
    return _left.min() >= _right;
  }

  protected void setSubjectTrue() throws Failure
  {
    // left < right
    _left.setMax(_right-1);
  }

  protected void setSubjectFalse() throws Failure
  {
    // left >= right
    _left.setMin(_right);
  }

  public boolean isLinear(){
    return (_left.isLinear());
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression
  {
    return (_left.neg().add(_right)).calcCoeffs(map, factor);
  }

} // ~IntBoolExpLessValue
