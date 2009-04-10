package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.FloatEvent;
import com.exigen.ie.constrainer.FloatExp;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>(1/FloatExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>FloatVar var = constrainer.addFloatVar(min,max,name);
 *  <p>
 *  FloatExp exp = var.inverse();
 *  </code>
 */
public final class FloatExpInverse extends FloatExpImpl
{
  private FloatExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN, MAX,
                                           MAX, MIN,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                         };

  class FloatExpInverseObserver extends ExpressionObserver
  {

    FloatExpInverseObserver()
    {
      super(event_map);
    }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      FloatEvent e = (FloatEvent) event;

      FloatEventInverse ev = FloatEventInverse.getEvent(e,FloatExpInverse.this);

      notifyObservers(ev);
    }

    public String toString()
    {
      return "FloatExpInverseObserver: "+_exp;
    }

    public Object master()
    {
      return FloatExpInverse.this;
    }

  } //~ FloatExpInverseObserver


  public FloatExpInverse(FloatExp exp)
  {
    super(exp.constrainer(),"");
    _exp = exp;
    _observer = new FloatExpInverseObserver();
    _exp.attachObserver(_observer);
  }

  public FloatExp inverse()
  {
    return _exp;
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public double max()
  {
    return FloatCalc.inverseMax(_exp.min(),_exp.max());
  }

  public double min()
  {
    return FloatCalc.inverseMin(_exp.min(),_exp.max());
  }

  public void setMax(double max) throws Failure
  {
//    System.out.println("++-++ Set max: " + max + " in " + this);
    double min = min();
    double expMin = FloatCalc.inverseMin(min,max);
    double expMax = FloatCalc.inverseMax(min,max);
    _exp.setMin(expMin);
    _exp.setMax(expMax);
  }

  public void setMin(double min) throws Failure
  {
//    System.out.println("++++ Set min: " + min + " in " + this);
    double max = max();
    double expMin = FloatCalc.inverseMin(min,max);
    double expMax = FloatCalc.inverseMax(min,max);
    _exp.setMin(expMin);
    _exp.setMax(expMax);
  }

  public void setValue(double value) throws Failure
  {
   double expMin = FloatCalc.inverseMin(value);
   double expMax = FloatCalc.inverseMax(value);
   _exp.setMin(expMin);
   _exp.setMax(expMax);
  }

  public String toString()
  {
    return "1/"+_exp + domainToString();
  }

  static class FloatEventInverse extends FloatEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new FloatEventInverse();
        }

    };

    static FloatEventInverse getEvent(FloatEvent event, FloatExpInverse exp)
    {
      FloatEventInverse ev = (FloatEventInverse) _factory.getElement();
      ev.init(event,exp);
      return ev;
    }

    FloatEvent _event;
    double _min,_max,_oldmin,_oldmax;
    int _type;

    public String name()
    {
      return "FloatEventInverse";
    }

    public void init(FloatEvent e, FloatExpInverse exp)
    {
      exp(exp);

      _min = FloatCalc.inverseMin(e.min(),e.max());
      _max = FloatCalc.inverseMax(e.min(),e.max());
      _oldmin = FloatCalc.inverseMin(e.oldmin(),e.oldmax());
      _oldmax = FloatCalc.inverseMax(e.oldmin(),e.oldmax());
      FloatCalc.doAssert(_min>=_oldmin,"_min>=_oldmin");
      FloatCalc.doAssert(_max<=_oldmax,"_max>=_oldmax");

      _event = e;
      int type;
      _type = type = e.type();

      _type &= (MAX | MIN);
      if(_max < _oldmax)
        _type |= MAX;
      if(_min > _oldmin)
        _type |= MIN;
    }

    public int type()
    {
      return _type;
    }

    public double min()
    {
      return _min;
    }

    public double max()
    {
      return _max;
    }

    public double oldmin()
    {
      return _oldmin;
    }

    public double oldmax()
    {
      return _oldmax;
    }

  } //~ FloatEventInverse

} // ~FloatExpInverse
