package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Observer;
import com.exigen.ie.constrainer.Subject;
/**
 * An implementation of the expression: <code>(IntExp == IntExp + offset)</code>.
 */
public final class IntBoolExpEqExp extends IntBoolExpForSubject
{
  private IntExp _exp1, _exp2;
  private int _offset;
  private Observer _observer;

  final class ObserverMinMax extends Observer
  {
    public int subscriberMask()
    {
      return ALL;
    }

    public void update(Subject exp, EventOfInterest interest)
                    throws Failure
    {
/*
      IntEvent event = (IntEvent)interest;
      if(isTrue() && event.isRemoveEvent())
      {
        int max = event.numberOfRemoves();
        for(int i=0; i < max; ++i)
        {
//          if (exp == _exp1)
            _exp2.removeValue(event.removed(i)-_offset);
//          else if (exp == _exp2)
            _exp1.removeValue(event.removed(i)+_offset);
        }
      }
*/
      setDomainMinMax();
    }

    public Object master()
    {
      return IntBoolExpEqExp.this;
    }

  } // ~ObserverMinMax


  public IntBoolExpEqExp(IntExp exp1, IntExp exp2)
  {
    this(exp1,exp2,0);
  }

  public IntBoolExpEqExp(IntExp exp1, IntExp exp2, int offset)
  {
    super(exp1.constrainer());

    _exp1 = exp1;
    _exp2 = exp2;
    _offset = offset;

    if (constrainer().showInternalNames())
    {
      if(offset == 0)
        _name = "(" + exp1.name() + "==" + exp2.name() + ")";
      else if(offset>0)
        _name = "(" + exp1.name() + "==" + exp2.name() + "+" + offset + ")";
      else
        _name = "(" + exp1.name() + "==" + exp2.name() + offset + ")";
    }

    setDomainMinMaxSafe();

    _observer = new ObserverMinMax();
    _exp1.attachObserver(_observer);
    _exp2.attachObserver(_observer);
  }

  protected boolean isSubjectTrue()
  {
    // both are bound and equals
    return _exp1.min() == _exp2.max() + _offset &&
           _exp1.max() == _exp2.min() + _offset;
  }

  protected boolean isSubjectFalse()
  {
    // exp1 > exp2 || exp1 < exp2
    return _exp1.min() > _exp2.max() + _offset ||
           _exp1.max() < _exp2.min() + _offset;
  }

  protected void setSubjectTrue() throws Failure
  {
    // exp1 == exp2
    _exp1.setMax(_exp2.max()+_offset);
    _exp2.setMax(_exp1.max()-_offset);
    _exp1.setMin(_exp2.min()+_offset);
    _exp2.setMin(_exp1.min()-_offset);
  }

  protected void setSubjectFalse() throws Failure
  {
    // exp1 != exp2
    if(_exp2.bound())
      _exp1.removeValue(_exp2.value() + _offset);
    if(_exp1.bound())
      _exp2.removeValue(_exp1.value() - _offset);
  }

//  public Constraint asConstraint()
//  {
//    return _exp1.equals(_exp2,_offset);
//  }
  public boolean isLinear(){
    return (_exp1.isLinear() && _exp2.isLinear());
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return (_exp2.sub(_exp1).sub(_offset)).calcCoeffs(map, factor);
  }
} // ~IntBoolExpEqExp
