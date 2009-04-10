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
 * An implementation of the expression: <code>(FloatExp + value)</code>.
 */
public final class FloatExpAddValue extends FloatExpImpl
{
  private FloatExp           _exp;
  private double             _value;
  private ExpressionObserver _observer;

  class FloatExpAddValueObserver extends ExpressionObserver
  {

    FloatExpAddValueObserver()
    {
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventAddValue ev = FloatEventAddValue.getEvent(e, _value);
      ev.exp(FloatExpAddValue.this);

      notifyObservers(ev);

    }

      public String toString()
      {
        return "FloatExpAddValueObserver: "+_exp+"+"+_value;
      }

      public Object master()
      {
        return FloatExpAddValue.this;
      }

  } //~ FloatExpAddValueObserver


  public FloatExpAddValue(FloatExp exp, double value)
  {
    super(exp.constrainer(),"");//exp.name()+"+"+value);
    _exp = exp;
    _value = value;
    _observer = new FloatExpAddValueObserver();
    _exp.attachObserver(_observer);
  }

  /**
   * Overloaded optimized implementation of the add(double)
   */
  public FloatExp add(double value)
  {
    return _exp.add(_value + value);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return _exp.max()+_value;
  }

  public double min()
  {
    return _exp.min() + _value;
  }

  public void setMax(double max) throws Failure
  {
    _exp.setMax(max - _value);
  }

  public void setMin(double min) throws Failure
  {
    _exp.setMin(min - _value);
  }

  public void setValue(double value) throws Failure
  {
    _exp.setValue(value - _value);
  }

//  public void removeValue(double value) throws Failure
//	{
//    _exp.removeValue(value - _value);
// 	}

  public double size()
  {
    return _exp.size();
  }

  public String toString()
  {
    return "("  +_exp + " + " +_value + ")";
  }
/*
  public double value() throws Failure
  {
    if (!bound())
      constrainer().fail("Attempt to get value of the unbound expression "+this);
    return _exp.value()+_value;
  }
*/
  static final class FloatEventAddValue extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventAddValue();
        }

    };

    static FloatEventAddValue getEvent(FloatEvent event, double value)
    {
      FloatEventAddValue ev = (FloatEventAddValue) _factory.getElement();
      ev.init(event, value);
      return ev;
    }


    double  _value;
    FloatEvent _event;

    public void init(FloatEvent e, double value)
    {
      _event = e;
      _value = value;
    }

    public int type()
    {
      return _event.type();
    }

    public String name()
    {
      return "Event FloatAddValue";
    }

    public double min()
    {
      return _event.min() + _value;
    }

    public double max()
    {
      return _event.max() + _value;
    }

    public double oldmin()
    {
      return _event.oldmin() + _value;
    }

    public double oldmax()
    {
      return _event.oldmax() + _value;
    }

  } //~ FloatEventAddValue

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression
  {
    return _exp.calcCoeffs(map, factor) + _value*factor;
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

} // ~FloatExpAddValue
