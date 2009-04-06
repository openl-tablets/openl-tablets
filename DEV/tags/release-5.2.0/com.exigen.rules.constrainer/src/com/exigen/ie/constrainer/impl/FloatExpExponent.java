package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>exp(FloatExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.exp();
 *  </code>
 */
public final class FloatExpExponent extends FloatExpImpl
{
  private FloatExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class FloatExpExponentObserver extends ExpressionObserver
  {

    FloatExpExponentObserver()
    {
      super(event_map);
    }


    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventExponent ev = FloatEventExponent.getEvent(e,FloatExpExponent.this);

      notifyObservers(ev);
    }


    public String toString()
    {
      return "FloatExpExponentObserver: "+_exp;
    }

    public Object master()
    {
      return FloatExpExponent.this;
    }
  } //~ FloatExpExponentObserver

  static final class FloatEventExponent extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventExponent();
        }

    };

    static FloatEventExponent getEvent(FloatEvent event, FloatExp exp)
    {
      FloatEventExponent ev = (FloatEventExponent) _factory.getElement();
      ev.init(event,exp);
      return ev;
    }

    FloatEvent _event;

    int _type = 0;

    void init(FloatEvent event, FloatExp exp_)
    {
      exp(exp_);
      _event = event;
      _type = 0;

      if (max() < oldmax())
      {
        _type |= MAX;
      }

      if (min() > oldmin())
      {
        _type |= MIN;
      }

      if (min() == max())
        _type |= VALUE;
    }


    public int type()
    {
      return _type;
    }


    public double oldmax()
    {
      return Math.exp(_event.oldmax());
    }

    public double oldmin()
    {
      return Math.exp(_event.oldmin());
    }

    public double max()
    {
      return Math.exp(_event.max());
    }

    public double min()
    {
      return Math.exp(_event.min());
    }


    public String name()
    {
      return "FloatEventExponent";
    }

  }

  public FloatExpExponent(FloatExp exp)
  {
    super(exp.constrainer(),"");//"exp("+exp.name()+")");
    _exp = exp;
    _observer = new FloatExpExponentObserver();
    _exp.attachObserver(_observer);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return Math.exp(_exp.max());
  }

  public double min()
  {
    return Math.exp(_exp.min());
  }

  public void setMax(double max) throws Failure
  {
    if(max < 0)
      constrainer().fail("max < 0");

    _exp.setMax(Math.log(max));
  }

  public void setMin(double min) throws Failure
  {
    if (min <= 0)
      return;

    _exp.setMin(Math.log(min));
  }

  public void setValue(double value) throws Failure
  {
    if(value <= 0)
      constrainer().fail("value <= 0");

    _exp.setValue(Math.log(value));
  }

  public String toString()
  {
    return "exp(" + _exp + ")" + domainToString();
  }

} // ~FloatExpExponent
