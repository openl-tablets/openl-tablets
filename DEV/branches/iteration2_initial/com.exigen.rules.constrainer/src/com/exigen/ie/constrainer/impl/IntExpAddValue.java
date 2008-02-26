package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;
//
//: IntExpAddValue.java
//
/**
 * An implementation of the expression: <code>(IntExp + value)</code>.
 */
public final class IntExpAddValue extends IntExpImpl
{
  private IntExp	           _exp;
  private int	               _value;
  private ExpressionObserver _observer;

  class ExpAddValueObserver extends ExpressionObserver
  {
    IntExp _exp_this;

    ExpAddValueObserver(IntExp exp_this)
    {
      _exp_this = exp_this;
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent) event;

      IntEventAddValue ev = IntEventAddValue.getEvent(e, _value);
      ev.exp(_exp_this);

      notifyObservers(ev);

    }


      public String toString()
      {
        return "ExpAddValueObserver: "+_exp+"+"+_value;
      }

      public Object master()
      {
        return IntExpAddValue.this;
      }

  } //~ ExpAddValueObserver


  public IntExpAddValue(IntExp exp, int value)
  {
    super(exp.constrainer());

    if(constrainer().showInternalNames())
    {
      _name = "("+exp.name()+"+"+value+")";
    }

    _exp = exp;
    _value = value;
    _observer = new ExpAddValueObserver(this);
    _exp.attachObserver(_observer);
  }

  public IntExp add(int value)
  {
    return _exp.add(_value + value);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public boolean contains(int value)
  {
    return _exp.contains(value - _value);
  }

  public int max()
  {
    return _exp.max()+_value;
  }

  public int min()
  {
    return _exp.min() + _value;
  }

  public void setMax(int max) throws Failure
  {
    _exp.setMax(max - _value);
  }

  public void setMin(int min) throws Failure
  {

//    System.out.println("++++ Set min: " + min + " in " + this);

    _exp.setMin(min - _value);
  }

  public void setValue(int value) throws Failure
  {
    _exp.setValue(value - _value);
  }

  public void removeValue(int value) throws Failure
  {
    _exp.removeValue(value - _value);
  }

  public int size()
  {
    return _exp.size();
  }

  public boolean bound()
  {
    return _exp.bound();
  }

  public int value() throws Failure
  {
    if (!_exp.bound())
      constrainer().fail("Attempt to get value of the unbound expression "+this);
    return _exp.value() + _value;
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression
  {
    return _exp.calcCoeffs(map, factor) + _value*factor;
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

  static final class IntEventAddValue extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventAddValue();
        }

    };

    static IntEventAddValue getEvent(IntEvent event, int value)
    {
      IntEventAddValue ev = (IntEventAddValue) _factory.getElement();
      ev.init(event, value);
      return ev;
    }


    int  _value;
    IntEvent _event;

    public void init(IntEvent e, int value)
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
      return "Event AddValue";
    }

    public int removed(int i)
    {
      return _event.removed(i) + _value;
    }


    public int min()
    {
      return _event.min() + _value;
    }

    public int max()
    {
      return _event.max() + _value;
    }

    public int oldmin()
    {
      return _event.oldmin() + _value;
    }


    public int oldmax()
    {
      return _event.oldmax() + _value;
    }

    public int numberOfRemoves()
    {
        return _event.numberOfRemoves();
    }

  } // ~IntEventAddValue

} // ~IntExpAddValue
