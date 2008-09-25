package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>(IntExp < IntExp + offset)</code>.
 */
public class IntBoolExpLessExp extends IntBoolExpForSubject
{
  protected IntExp _left, _right;
  protected int _offset;
  private Observer _observer;

  final class ObserverMinMax extends Observer
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
      return IntBoolExpLessExp.this;
    }

  } // ~ObserverMinMax


  public IntBoolExpLessExp(IntExp left, IntExp right)
  {
    this(left,right,0);
  }

  public IntBoolExpLessExp(IntExp left, IntExp right, int offset)
  {
    super(left.constrainer());

    _left = left;
    _right = right;
    _offset = offset;

    if(constrainer().showInternalNames())
    {
      _name = "(" + left.name() + "<" + right.name() + "+" + offset + ")";
    }

    setDomainMinMaxSafe();

    _observer = new ObserverMinMax();
    _left.attachObserver(_observer);
    _right.attachObserver(_observer);
  }

  protected boolean isSubjectTrue()
  {
    return _left.max() < _right.min() + _offset;
  }

  protected boolean isSubjectFalse()
  {
    return _left.min() >= _right.max() + _offset;
  }

  protected void setSubjectTrue() throws Failure
  {
    // left < right + offset
    _left.setMax(_right.max()+_offset-1);
    _right.setMin(_left.min()-_offset+1);
  }

  protected void setSubjectFalse() throws Failure
  {
    // left >= right + offset
    _left.setMin(_right.min()+_offset);
    _right.setMax(_left.max()-_offset);
  }

   public boolean isLinear(){
    return (_left.isLinear() && _right.isLinear());
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return (_right.sub(_left).sub(_offset)).calcCoeffs(map, factor);
  }

} // ~IntBoolExpLessExp
