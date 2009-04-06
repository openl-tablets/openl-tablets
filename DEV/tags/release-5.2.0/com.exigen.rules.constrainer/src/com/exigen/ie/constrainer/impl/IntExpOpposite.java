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
//: IntExpOpposite.java
//
/**
 * An implementation of the expression: <code>(-IntExp)</code>.
 */
public final class IntExpOpposite extends IntExpImpl
{
  private IntExp	           _exp;
  private ExpressionObserver _observer;

  static final private int[] event_map = { MIN, MAX,
                                           MAX, MIN,
                                           VALUE, VALUE,
                                           REMOVE, REMOVE
                                         };

  class ExpOppositeObserver extends ExpressionObserver
  {
      IntExp _exp_this;

      ExpOppositeObserver(IntExp exp_this, int[] event_map)
      {
        super(event_map);
        _exp_this = exp_this;
      }

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent) event;

      IntEventOpposite ev = IntEventOpposite.getEvent(e);

      ev.exp(_exp_this);

      notifyObservers(ev);

    }

      public String toString()
      {
        return "ExpOppositeObserver: "+_exp;
      }

      public Object master()
      {
        return IntExpOpposite.this;
      }

  } //~ ExpOppositeObserver


  public IntExpOpposite(IntExp exp)
  {
    super(exp.constrainer());

    if(constrainer().showInternalNames())
    {
      _name = "(-"+exp.name()+")";
    }

    _exp = exp;
    _observer = new ExpOppositeObserver(this, event_map);
    _exp.attachObserver(_observer);
  }

// public IntExp negI()
// {
//    return _exp;
// }

 /** added by ET 02.12.03
  *  for optimization
  */
 public IntExp neg()
 {
    return _exp;
 }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public boolean contains(int value)
  {
    return _exp.contains(-value);
  }

  public int max()
  {
    return -_exp.min();
  }

  public int min()
  {
    return -_exp.max();
  }

  public void setMax(int max) throws Failure
  {
   // System.out.println("++-++ Set max: " + max + " in " + this);

    _exp.setMin(-max);
  }

  public void setMin(int min) throws Failure
  {
    //System.out.println("++++ Set min: " + min + " in " + this);

    _exp.setMax(-min);
  }

  public void setValue(int value) throws Failure
  {
    _exp.setValue(-value);
  }

  public void removeValue(int value) throws Failure
  {
    _exp.removeValue(-value);
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
    return -_exp.value();
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _exp.calcCoeffs(map, -1*factor);
  }

  static class IntEventOpposite extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventOpposite();
        }

    };

    static IntEventOpposite getEvent(IntEvent event)
    {
      IntEventOpposite ev = (IntEventOpposite) _factory.getElement();
      ev.init(event);
      return ev;
    }

    IntEvent _event;
    int _type;

    public String name()
    {
      return "Event Opposite";
    }



    public void init(IntEvent e)
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



    public int removed(int i)
    {
      return -_event.removed(i);
    }


    public int min()
    {
      return -_event.max();
    }

    public int max()
    {
      return -_event.min();
    }

    public int oldmin()
    {
      return -_event.oldmax();
    }


    public int oldmax()
    {
      return -_event.oldmin();
    }

    public int numberOfRemoves()
    {
        return _event.numberOfRemoves();
    }


}

} //eof IntExpOpposite
