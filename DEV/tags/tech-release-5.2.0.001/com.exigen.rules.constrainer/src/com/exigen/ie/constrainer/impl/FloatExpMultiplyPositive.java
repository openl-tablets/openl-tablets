package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;
/**
 * An implementation of the expression: <code>(FloatExp * value)</code> for positive "value".
 */
public final class FloatExpMultiplyPositive extends FloatExpImpl
{
  private FloatExp	         _exp;
  private double             _value;
  private ExpressionObserver _observer;

  class FloatExpMultiplyPositiveObserver extends ExpressionObserver
  {

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventMulPositiveValue ev = FloatEventMulPositiveValue.getEvent(e, _value);

      notifyObservers(ev);

    }

    public String toString()
    {
      return "FloatExpMultiplyPositiveObserver: "+_exp+"+"+_value;
    }

    public Object master()
    {
      return FloatExpMultiplyPositive.this;
    }

  } //~ FloatExpMultiplyPositiveObserver


  public FloatExpMultiplyPositive(FloatExp exp, double value)
  {
    super(exp.constrainer(),"");//exp.name()+"+"+value);
    if ( value <= 0 )
      abort("negative value in FloatExpMultiplyPositive");
    _exp = exp;
    _value = value;
    _observer = new FloatExpMultiplyPositiveObserver();
    _exp.attachObserver(_observer);
  }


  public FloatExp mul(double value)
  {
    return _exp.mul(_value * value);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return _exp.max() * _value;
  }

  public double min()
  {
    return _exp.min() * _value;
  }

  public void setMax(double max) throws Failure
  {
    _exp.setMax(max/_value); // may fail
  }

  public void setMin(double min) throws Failure
  {
    _exp.setMin(min/_value); // may fail
  }

  public void setValue(double value) throws Failure
  {
    _exp.setValue(value/_value);
  }

  public String toString()
  {
    domainToString();
    return name() + "(" + _exp + "x" + _value + domainToString() + ")";
  }

  static class FloatEventMulPositiveValue extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventMulPositiveValue();
        }

    };

    static FloatEventMulPositiveValue getEvent(FloatEvent event, double value)
    {
      FloatEventMulPositiveValue ev = (FloatEventMulPositiveValue) _factory.getElement();
      ev.init(event, value);
      return ev;
    }

    private double  _value;
    private FloatEvent _event;

    public String name()
    {
      return "FloatEventMulPositiveValue";
    }

    public void init(FloatEvent e, double value)
    {
      _event = e;
      _value = value;
    }

    public int type()
    {
      return _event.type();
    }

    public double min()
    {
      return _event.min() * _value;
    }

    public double max()
    {
      return _event.max() * _value;
    }

    public double oldmin()
    {
      return _event.oldmin() * _value;
    }

    public double oldmax()
    {
      return _event.oldmax() * _value;
    }

  } //~ FloatEventMulPositiveValue

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _exp.calcCoeffs(map, factor*_value);
  }

} // ~FloatExpMultiplyPositive
