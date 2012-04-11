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
 * An implementation of the expression: <code>(-FloatExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.negF();
 *  </code>
 */
public final class FloatExpOpposite extends FloatExpImpl
{
  private FloatExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN, MAX,
                                           MAX, MIN,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                         };

  class FloatExpOppositeObserver extends ExpressionObserver
  {

    FloatExpOppositeObserver()
    {
      super(event_map);
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventOpposite ev = FloatEventOpposite.getEvent(e);
      ev.exp(FloatExpOpposite.this);

      notifyObservers(ev);
    }

    public String toString()
    {
      return "FloatExpOppositeObserver: "+_exp;
    }

    public Object master()
    {
      return FloatExpOpposite.this;
    }

  } //~ FloatExpOppositeObserver


  public FloatExpOpposite(FloatExp exp)
  {
    super(exp.constrainer(),"");
    _exp = exp;
    _observer = new FloatExpOppositeObserver();
    _exp.attachObserver(_observer);
  }

  public FloatExp negF()
  {
    return _exp;
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return -_exp.min();
  }

  public double min()
  {
    return -_exp.max();
  }

  public void setMax(double max) throws Failure
  {
   // System.out.println("++-++ Set max: " + max + " in " + this);

    _exp.setMin(-max);
  }

  public void setMin(double min) throws Failure
  {

    //System.out.println("++++ Set min: " + min + " in " + this);

    _exp.setMax(-min);
  }

  public void setValue(double value) throws Failure
  {
    _exp.setValue(-value);
  }

  public String toString()
  {
    return "-"+_exp + domainToString();
  }

  static class FloatEventOpposite extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventOpposite();
        }

    };

    static FloatEventOpposite getEvent(FloatEvent event)
    {
      FloatEventOpposite ev = (FloatEventOpposite) _factory.getElement();
      ev.init(event);
      return ev;
    }

    FloatEvent _event;
    int _type;

    public String name()
    {
      return "FloatEventOpposite";
    }


    public void init(FloatEvent e)
    {
      _event = e;
      int type;
      _type = type = e.type();

      _type |= MAX | MIN;
      if ((type & MIN ) == 0)
        _type &= ~MAX;
      if ((type & MAX ) == 0)
        _type &= ~MIN;
    }

    public int type()
    {
      return _type;
    }

    public double min()
    {
      return -_event.max();
    }

    public double max()
    {
      return -_event.min();
    }

    public double oldmin()
    {
      return -_event.oldmax();
    }

    public double oldmax()
    {
      return -_event.oldmin();
    }

  } //~ FloatEventOpposite


  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _exp.calcCoeffs(map, -1*factor);
  }

} // ~FloatExpOpposite
