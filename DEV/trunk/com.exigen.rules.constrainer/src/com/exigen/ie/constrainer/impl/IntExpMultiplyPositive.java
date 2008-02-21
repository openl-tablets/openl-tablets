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
//: IntExpMultiplyPositive.java
//
/**
 * An implementation of the expression: <code>(IntExp * value)</code> for positive "value".
 */
public final class IntExpMultiplyPositive extends IntExpImpl
{
  private IntExp	           _exp;
  private int	               _value;
  private ExpressionObserver _observer;

  class ExpMultiplyPositiveObserver extends ExpressionObserver
  {

    public void update(Subject exp, EventOfInterest event)
      throws Failure
    {
      IntEvent e = (IntEvent) event;

      IntEventMulPositiveValue ev = IntEventMulPositiveValue.getEvent(e, _value);

      notifyObservers(ev);

    }

      public String toString()
      {
        return "ExpMultiplyPositiveObserver: "+_exp+"*"+_value;
      }


      public Object master()
      {
        return IntExpMultiplyPositive.this;
      }

  } //~ ExpMultiplyPositiveObserver


  public IntExpMultiplyPositive(IntExp exp, int value)
  {
    super(exp.constrainer());
    if ( value <= 0 )
      abort("negative value in IntExpMultiplyPositive");
    _exp = exp;
    _value = value;

    if(constrainer().showInternalNames())
    {
      _name = "("+exp.name()+"*"+value+")";
    }

    _observer = new ExpMultiplyPositiveObserver();
    _exp.attachObserver(_observer);
  }


  public IntExp mul(int value)
  {
    return _exp.mul(_value * value);
  }

  public void onMaskChange()
  {
    _observer.publish(publisherMask(),_exp);
  }

  public boolean contains(int value)
  {
    if (value % _value != 0)
      return false;
    return _exp.contains(value/_value);
  }

  public int max()
  {
    return _exp.max() * _value;
  }

  public int min()
  {
    return _exp.min() * _value;
  }

  public void setMax(int max) throws Failure
  {
    int Pmax = max;
    int Min, Max, delta, q ;


   //(a/b)*b + (a%b) = a - java spec
   // 5/3 = 1(2)
   // -5/3 = -1(-2)
   // -5/-3 = 1(-2)
   // 5/-3  = -1(2)

   // P = (71, 90), val = 10, var=(8,9)
   // P = (70, 89), val = 10, var=(7,8)
   // P = (-80, -61), val = 10, var = (-8, -7)
   // P = (-79, -60), val = 10, var = (-7, -6)


    int v = _value;

   //
   //max: Pmax = q * V + d
   // Xmax * V <= q * V + d < (Xmax + 1) * V
   // Xmax < q + 1
   // a) d >= 0
   // Xmax = q
   // b) d < 0
   // Xmax = q -1

    q = Pmax / v;
    delta = Pmax - q * v;
    Max = delta >= 0 ? q : q - 1;

    _exp.setMax(Max); // may fail
  }

  public void setMin(int min) throws Failure
  {
    if (min < min())
      return;

 //   System.out.println("++++ Set min: " + min + " in " + this);

    if (min > max())
      constrainer().fail("Mul Positive Set Min");

    int Pmin = min;
    int Min, delta, q ;


   //(a/b)*b + (a%b) = a - java spec
   // 5/3 = 1(2)
   // -5/3 = -1(-2)
   // -5/-3 = 1(-2)
   // 5/-3  = -1(2)

   // P = (71, 90), val = 10, var=(8,9)
   // P = (70, 89), val = 10, var=(7,8)
   // P = (-80, -61), val = 10, var = (-8, -7)
   // P = (-79, -60), val = 10, var = (-7, -6)


  int v = _value;

   // V > 0
   // min: Pmin = q * V + d,   V > d > -V
   // Xmin * V >= q * V + d > (Xmin - 1) * V
   // Xmin > q - 1
   // a) d > 0
   // Xmin * V > q * V
   // Xmin > q >> Xmin = q + 1
   //
   // b) d <= 0
   //  q > Xmin - 1
   //  q+1 > Xmin >> Xmin = q

    q = Pmin / v;
    delta = Pmin - q * v;
    Min = delta > 0 ? q + 1 : q;

    _exp.setMin(Min); // may fail
  }

  public void setValue(int value) throws Failure
  {
    if (value % _value != 0)
      constrainer().fail("invalid setValue in IntExpMultiplyPositiveImpl");
    _exp.setValue(value/_value);
  }

  public void removeValue(int value) throws Failure
  {
    if (value % _value != 0)
      return;
    _exp.removeValue(value/_value);
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
    return _exp.value() * _value;
  }

  public boolean isLinear(){
    return _exp.isLinear();
  }

  public double calcCoeffs(Map map, double factor) throws NonLinearExpression{
    return _exp.calcCoeffs(map, factor*_value);
  }

  static class IntEventMulPositiveValue extends IntEvent
  {

    static ReusableFactory _factory = new ReusableFactory()
    {
        protected Reusable createNewElement()
        {
          return new IntEventMulPositiveValue();
        }

    };

    static IntEventMulPositiveValue getEvent(IntEvent event, int value)
    {
      IntEventMulPositiveValue ev = (IntEventMulPositiveValue) _factory.getElement();
      ev.init(event, value);
      return ev;
    }



    int  _value;
    IntEvent _event;

    public String name()
    {
      return "Event MulValue";
    }



    public void init(IntEvent e, int value)
    {
      _event = e;
      _value = value;
    }

    public int type()
    {
      return _event.type();
    }



    public int removed(int i)
    {
      return _event.removed(i) * _value;
    }


    public int min()
    {
      return _event.min() * _value;
    }

    public int max()
    {
      return _event.max() * _value;
    }

    public int oldmin()
    {
      return _event.oldmin() * _value;
    }


    public int oldmax()
    {
      return _event.oldmax() * _value;
    }

    public int numberOfRemoves()
    {
        return _event.numberOfRemoves();
    }


  }

} //eof IntExpMultiplyPositive
