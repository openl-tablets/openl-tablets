package com.exigen.ie.constrainer.impl;
import com.exigen.ie.constrainer.EventOfInterest;
import com.exigen.ie.constrainer.Failure;
import com.exigen.ie.constrainer.IntExp;
import com.exigen.ie.constrainer.Subject;
import com.exigen.ie.tools.Reusable;
import com.exigen.ie.tools.ReusableFactory;

/**
 * An implementation of the expression: <code>sqr(IntExp)</code>.
 *  <p>
 *  Examples:
 *  <p>
 *  <code>IntVar var = constrainer.addIntVar(min,max,name);
 *  <p>
 *  IntExp exp = var.sqr();
 *  </code>
 */
public final class IntExpSqr extends IntExpImpl
{
  private IntExp	         _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN | MAX, MIN,
                                           MIN | MAX, MAX,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                          };


  class IntExpSqrObserver extends ExpressionObserver
  {

    IntExpSqrObserver()
    {
      super(event_map);
    }


    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent) event;

      IntEventSqr ev = IntEventSqr.getEvent(e,IntExpSqr.this);

      notifyObservers(ev);
    }


    public String toString()
    {
      return "IntExpSqrObserver: "+_exp;
    }

    public Object master()
    {
      return IntExpSqr.this;
    }
  } //~ IntExpSqrObserver

  static final class IntEventSqr extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventSqr();
        }

    };

    static IntEventSqr getEvent(IntEvent event, IntExp exp)
    {
      IntEventSqr ev = (IntEventSqr) _factory.getElement();
      ev.init(event,exp);
      return ev;
    }

    IntEvent _event;

    int _type = 0;

    void init(IntEvent event, IntExp exp_)
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


    public int oldmax()
    {
      return IntCalc.sqrMax(_event.oldmin(), _event.oldmax());
    }

    public int oldmin()
    {
      return IntCalc.sqrMin(_event.oldmin(), _event.oldmax());
    }

    public int max()
    {
      return IntCalc.sqrMax(_event.min(), _event.max());
    }

    public int min()
    {
      return IntCalc.sqrMin(_event.min(), _event.max());
    }

    public int numberOfRemoves()
    {
      return 0;
    }

    public int removed(int i)
    {
      return 0;
    }

    public String name()
    {
      return "IntEventSqr";
    }

  }

  public IntExpSqr(IntExp exp)
  {
    super(exp.constrainer());
    _exp = exp;

    if(constrainer().showInternalNames())
    {
      _name = "("+exp.name()+"*"+exp.name()+")";
    }

    _observer = new IntExpSqrObserver();
    _exp.attachObserver(_observer);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public int max()
  {
    return IntCalc.sqrMax(_exp.min(), _exp.max());
  }

  public int min()
  {
    return IntCalc.sqrMin( _exp.min(), _exp.max());
  }

  public void setMax(int max) throws Failure
  {
    if(max < 0)
      constrainer().fail("max < 0");

    int expMinMax = (int)Math.sqrt(max);
    _exp.setMax(expMinMax);
    _exp.setMin(-expMinMax);
  }

  public void setMin(int min) throws Failure
  {
    if (min <= 0)
      return;

    int expMinMax = (int)Math.sqrt(min);

    if(expMinMax*expMinMax == min)
      expMinMax--;

    for (int i = -expMinMax; i <= expMinMax; i++)
    {
      _exp.removeValue(i);
    }
  }

  public void setValue(int value) throws Failure
  {
    if(value < 0)
      constrainer().fail("value < 0");

    int sqrtValue = IntCalc.sqrtInt(value);
    if(sqrtValue < 0)
      constrainer().fail("value is not a square");

    _exp.setValue(sqrtValue);
  }

  public void removeValue(int value) throws Failure
  {
    if (value < 0)
      return;

    int sqrtValue = IntCalc.sqrtInt(value);
    if(sqrtValue < 0)
      return;

    _exp.removeValue(sqrtValue);
    _exp.removeValue(-sqrtValue);
  }

  public String toString()
  {
    return "sqr(" + _exp + ")" + domainToString();
  }

} // ~IntExpSqr
