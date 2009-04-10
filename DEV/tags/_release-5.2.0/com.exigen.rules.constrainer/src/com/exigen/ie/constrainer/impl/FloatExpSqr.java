package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>sqr(FloatExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.sqr();
 *  </code>
 */
public final class FloatExpSqr extends FloatExpImpl
{
  private FloatExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class FloatExpSqrObserver extends ExpressionObserver
  {

    FloatExpSqrObserver()
    {
      super(event_map);
    }


    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventSqr ev = FloatEventSqr.getEvent(e,FloatExpSqr.this);

      notifyObservers(ev);
    }


    public String toString()
    {
      return "FloatExpSqrObserver: "+_exp;
    }

    public Object master()
    {
      return FloatExpSqr.this;
    }
  } //~ FloatExpSqrObserver

  static final class FloatEventSqr extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventSqr();
        }

    };

    static FloatEventSqr getEvent(FloatEvent event, FloatExp exp)
    {
      FloatEventSqr ev = (FloatEventSqr) _factory.getElement();
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
      return FloatCalc.sqrMax(_event.oldmin(), _event.oldmax());
    }

    public double oldmin()
    {
      return FloatCalc.sqrMin(_event.oldmin(), _event.oldmax());
    }

    public double max()
    {
      return FloatCalc.sqrMax(_event.min(), _event.max());
    }

    public double min()
    {
      return FloatCalc.sqrMin(_event.min(), _event.max());
    }


    public String name()
    {
      return "FloatEventSqr";
    }

  }

  public FloatExpSqr(FloatExp exp)
  {
    super(exp.constrainer(),"");//"sqr("+exp.name()+")");
    _exp = exp;
    _observer = new FloatExpSqrObserver();
    _exp.attachObserver(_observer);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return FloatCalc.sqrMax(_exp.min(), _exp.max());
  }

  public double min()
  {
    return FloatCalc.sqrMin( _exp.min(), _exp.max());
  }

  public void setMax(double max) throws Failure
  {
    if(max < 0)
      constrainer().fail("max < 0");

    double expMax = Math.sqrt(max);
    _exp.setMax(expMax);
    _exp.setMin(-expMax);
  }

  public void setMin(double min) throws Failure
  {
    if (min <= 0)
      return;
    // exclude range [-Math.sqrt(min)..Math.sqrt(min)] ???
  }

  public void setValue(double value) throws Failure
  {
    if(value < 0)
      constrainer().fail("value < 0");

    _exp.setValue(Math.sqrt(value));
  }

  public String toString()
  {
    return "sqr(" + _exp + ")" + domainToString();
  }

} // ~FloatExpSqr
