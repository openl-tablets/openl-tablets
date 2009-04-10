package com.exigen.ie.constrainer.impl;
import java.util.Map;

import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.NonLinearExpression;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;
/**
 * An implementation of the expression: <code>FloatExp(IntExp)</code>.
 */
public final class FloatExpIntExp extends FloatExpImpl
{
  private IntExp _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN, MIN,
                                           MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE,
                                         };

  /**
   * Class <code>FloatExpIntExpObserver</code> wrap event from integer
   * expression into float event and notify observers of float expression.
   */
  class FloatExpIntExpObserver extends ExpressionObserver
  {
      FloatExpIntExpObserver()
      {
        super(event_map);
      }

      public void update(Subject exp, EventOfInterest event)
        throws Failure
      {
        IntEvent e = (IntEvent) event;

        FloatEventIntEvent ev = FloatEventIntEvent.getEvent(e,FloatExpIntExp.this);

        notifyObservers(ev);
      }


      public String toString()
      {
        return "FloatExpIntExpObserver: ";
      }

      public Object master()
      {
        return FloatExpIntExp.this;
      }

  } //~ FloatExpIntExpObserver


  public FloatExpIntExp(IntExp exp)
  {
    super(exp.constrainer(),exp.name());
    _exp = exp;

    _observer = new FloatExpIntExpObserver();
    _exp.attachObserver(_observer);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return (double)_exp.max();
  }

  public double min()
  {
    return (double)_exp.min();
  }

  public void setMax(double max) throws Failure
  {
    if(max >= _exp.max())
      return;

    if(max < _exp.min())
      constrainer().fail("FloatExpIntExp.setMax()");

    // Truncate to negative infinity. Conversion to int Ok: max in [min()..max())
    int maxI = (int)Math.floor(max);

    _exp.setMax(maxI);
  }

  public void setMin(double min) throws Failure
  {
    if(min <= _exp.min())
      return;

    if(min > _exp.max())
      constrainer().fail("FloatExpIntExp.setMin()");

    // Truncate to positive infinity. Conversion to int Ok: min in (min()..max()].
    int minI = (int)Math.ceil(min);

    _exp.setMin(minI);
  }

  public void setValue(double value) throws Failure
  {
    int valueI = (int)value;

    if(valueI == value)
      _exp.setValue(valueI);
    else
      constrainer().fail("FloatExpIntExp.setValue(): bad integer value: "+value);
  }

//  public void removeValue(double value) throws Failure
//  {
//    int valueI = (int)value;
//
//    if (valueI == value)
//      _exp.removeValue(valueI);
//    else
//      constrainer().fail("removeValue() for FloatExpIntExp: not an integer value: "+value);
//  }

  /**
   * The methods bound() and value() can be implemented
   * following either semantic of the float expression (as in ILOG's Solver)
   * OR the underlyng integer expression.
   */
  private static final boolean AS_FLOAT_EXP = true;

  public boolean bound()
  {
    if(AS_FLOAT_EXP)
      return super.bound();

    return _exp.bound();
  }

  /**
   * Float expression is bound to the mean value in the interval associated with this expresion.
   */
  public double value() throws Failure
  {
    if(AS_FLOAT_EXP)
      return super.value();

    return (double)_exp.value();
  }

  public String toString()
  {
    return "Float(" + _exp + ")" ;
  }


  static final class FloatEventIntEvent extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventIntEvent();
        }

    };

    static FloatEventIntEvent getEvent(IntEvent event, FloatExp exp)
    {
      FloatEventIntEvent ev = (FloatEventIntEvent) _factory.getElement();
      ev.init(event,exp);
      return ev;
    }


    IntEvent _event;

    public String name()
    {
      return "Event FloatEventIntEvent";
    }


    public void init(IntEvent e, FloatExp exp_)
    {
      exp(exp_);
      _event = e;
    }

    public int type()
    {
      return _event.type();
    }

    public double min()
    {
      return (double)_event.min();
    }

    public double max()
    {
      return (double)_event.max();
    }

    public double oldmin()
    {
      return (double)_event.oldmin();
    }


    public double oldmax()
    {
      return (double)_event.oldmax();
    }


  } //~ FloatEventIntEvent

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _exp.calcCoeffs(map, factor);
  }

} //~ FloatExpIntExp
