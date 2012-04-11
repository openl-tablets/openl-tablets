package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>log(FloatExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.log();
 *  </code>
 */
public final class FloatExpLog extends FloatExpImpl
{
  private FloatExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class FloatExpLogObserver extends ExpressionObserver
  {

    FloatExpLogObserver()
    {
      super(event_map);
    }


    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventLog ev = FloatEventLog.getEvent(e,FloatExpLog.this);

      notifyObservers(ev);
    }


    public String toString()
    {
      return "FloatExpLogObserver: "+_exp;
    }

    public Object master()
    {
      return FloatExpLog.this;
    }
  } //~ FloatExpLogObserver

  static final class FloatEventLog extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventLog();
        }

    };

    static FloatEventLog getEvent(FloatEvent event, FloatExp exp)
    {
      FloatEventLog ev = (FloatEventLog) _factory.getElement();
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
      return Math.log(_event.oldmax());
    }

    public double oldmin()
    {
      return Math.log(_event.oldmin());
    }

    public double max()
    {
      return Math.log(_event.max());
    }

    public double min()
    {
      return Math.log(_event.min());
    }


    public String name()
    {
      return "FloatEventLog";
    }

  }

  public FloatExpLog(FloatExp exp)
  {
    super(exp.constrainer(),"");//"log("+exp.name()+")");
    _exp = exp;
    _observer = new FloatExpLogObserver();
    _exp.attachObserver(_observer);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return Math.log(_exp.max());
  }

  public double min()
  {
    return Math.log(_exp.min());
  }

  public void setMax(double max) throws Failure
  {
    _exp.setMax(Math.exp(max));
  }

  public void setMin(double min) throws Failure
  {
    _exp.setMin(Math.exp(min));
  }

  public void setValue(double value) throws Failure
  {
    _exp.setValue(Math.exp(value));
  }

  public String toString()
  {
    return "log(" + _exp + ")" + domainToString();
  }

} // ~FloatExpLog
